package com.lld.im.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.BaseErrorCode;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.config.AppConfig;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.GateWayErrorCode;
import com.lld.im.common.enums.ImUserTypeEnum;
import com.lld.im.common.exception.ApplicationExceptionEnum;
import com.lld.im.common.utils.SigAPI;
import com.lld.im.service.user.dao.ImUserDataEntity;
import com.lld.im.service.user.service.ImUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-30
 * @Description: 专门用来校验接口中的签名，操作人和appId是否匹配的类
 * @Version: 1.0
 */

@Component
public class IdentityCheck {

    private static Logger logger = LoggerFactory.getLogger(IdentityCheck.class);

    @Autowired
    ImUserService imUserService;

    @Autowired
    AppConfig appConfig;     // 实际生产中会专门用一张表来记录不同的app的密钥，我们的项目中在这里使用写在配置文件中的方法来实现

    @Autowired
    StringRedisTemplate stringRedisTemplate;  // 当校验一次成功后就可以存入redis，下一次就不需要再次解密了

    /**
     * 鉴权的方法
     * @param identifier
     * @param appId
     * @param userSig
     * @return  ApplicationExceptionEnum 当我们鉴权失败之后可以直接返回异常类
     */
    public ApplicationExceptionEnum checkUserSig(String identifier, String appId, String userSig){

        // 首先判断redis中是否已经有了该密钥的记录
        String cacheUserSig = stringRedisTemplate.opsForValue()
                .get(appId + ":" + Constants.RedisConstants.userSign + ":"
                        + identifier + userSig);
        if(!StringUtils.isBlank(cacheUserSig) && Long.valueOf(cacheUserSig)
                >  System.currentTimeMillis() / 1000 ){
            return BaseErrorCode.SUCCESS;
        }

        //获取秘钥
        String privateKey = appConfig.getPrivateKey();

        //根据appid + 秘钥创建sigApi
        SigAPI sigAPI = new SigAPI(Long.valueOf(appId), privateKey);

        // 调用SigAPI对象对userSign解密
        JSONObject jsonObject = sigAPI.decodeUserSig(userSig);

        // 取出解密后的appId和操作人和过期时间做匹配，不通过则提示错误
        Long expireTime = 0L;     // 过期时间
        Long expireSec = 0L;      // 过期的秒数
        String decoderAppId = "";     // 解密后的appId
        String decoderIdentifier = "";     // 解密后的操作人
        Long time = 0L;

        try{
            decoderAppId = jsonObject.getString("TLS.appId");
            decoderIdentifier = jsonObject.getString("TLS.identifier");
            String expireStr = jsonObject.get("TLS.expire").toString();
            String expireTimeStr = jsonObject.get("TLS.expireTime").toString();
            time = Long.valueOf(expireTimeStr);
            expireSec = Long.valueOf(expireStr) / 1000;     // 过期的秒数
            expireTime = Long.valueOf(expireTimeStr) + expireSec;      // 过期时间
        }catch(Exception e){
            e.printStackTrace();
            logger.error("checkUserSig-error:{}",e.getMessage());
        }

        // 判断操作人
        if(!decoderIdentifier.equals(identifier)){
            return GateWayErrorCode.USERSIGN_OPERATE_NOT_MATE;
        }
        // 判断appId
        if(!decoderAppId.equals(appId)){
            return GateWayErrorCode.USERSIGN_IS_ERROR;
        }
        // 判断过期的秒数，如果过期的秒数等于0，返回已过期
        if(expireSec == 0L){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }
        // 判断过期时间，如果小于系统时间，返回已过期
        if(expireTime < System.currentTimeMillis() / 1000 ){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        // 将userSig存入Redis中并设置过期时间  key:appId+"固定字符串"+userId+sign
        // 判断从appServer传过来的userSig和我么生成的userSig是否一直
        String genSig = sigAPI.genUserSig(identifier, expireSec,time,null);
        if (genSig.toLowerCase().equals(userSig.toLowerCase())) {
            String key = appId + ":" + Constants.RedisConstants.userSign + ":"
                    + identifier + userSig;
            Long etime = expireTime - System.currentTimeMillis() / 1000;
            stringRedisTemplate.opsForValue().set(
                    key, expireTime.toString(), etime, TimeUnit.SECONDS
            );
            // 校验本次请求是否是后台调用
            this.setIsAdmin(identifier,Integer.valueOf(appId));
            return BaseErrorCode.SUCCESS;
        }

        return BaseErrorCode.SUCCESS;
    }


    /**
     * 根据appid,identifier判断是否App管理员,并设置到RequestHolder
     * @param identifier
     * @param appId
     * @return
     */
    public void setIsAdmin(String identifier, Integer appId) {
        // 去DB或Redis中查找, 后面写
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(identifier, appId);
        if(singleUserInfo.isOk()){
            RequestHolder.set(singleUserInfo.getData().getUserType() == ImUserTypeEnum.APP_ADMIN.getCode());
        }else{
            RequestHolder.set(false);
        }
    }
}
