package com.lld.im.service.utils;

import com.lld.im.common.ResponseVO;
import com.lld.im.common.config.AppConfig;
import com.lld.im.common.utils.HttpRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-27
 * @Description: 在业务逻辑中实现业务回调
 * @Version: 1.0
 */
@Component
public class CallbackService {

    private Logger logger = LoggerFactory.getLogger(CallbackService.class);

    @Autowired
    HttpRequestUtils httpRequestUtils;

    @Autowired
    AppConfig appConfig;


    /***
     *  之后的回调，即我们处理完业务方法后通过之后回调告诉我们的业务系统我们的操作，不需要返回值
     * @param appId
     * @param callbackCommand
     * @param jsonBody
     */
    public void callback(Integer appId,String callbackCommand,String jsonBody) {
        /***
         *  参数说明：
         *  1. http请求要发送到的地址
         *  2. 返回值的类型
         *  3. 发送请求所需参数
         *  4. 发送请求的请求体
         *  5. 使用的字符集，不指定时默认是UTF-8
         */
        try {
            httpRequestUtils.doPost(appConfig.getCallbackUrl(),Object.class,builderUrlParams(appId,callbackCommand),jsonBody,null);
        } catch (Exception e) {
            logger.error("callback 回调{} : {}出现异常 ： {} ",callbackCommand , appId, e.getMessage());
        }
    }


    /***
     *  之前的回调，即在我们进行操作之前通过之前回调告诉我们的业务系统我们将要进行的操作
     *  需要返回值：返回业务系统的响应或者需要做的干预
     * @param appId
     * @param callbackCommand
     * @param jsonBody
     * @return
     */
    public ResponseVO beforeCallback(Integer appId,String callbackCommand,String jsonBody) {

        try {
            ResponseVO responseVO = httpRequestUtils.doPost("", ResponseVO.class, builderUrlParams(appId, callbackCommand),
                    jsonBody, null);
            return responseVO;
        } catch (Exception e) {
            logger.error("callback 之前 回调{} : {}出现异常 ： {} ",callbackCommand , appId, e.getMessage());
            // 如果抛出了异常则默认回调成功，继续执行
            return ResponseVO.successResponse();
        }
    }


    // 处理参数的方法
    public Map builderUrlParams(Integer appId,String command) {
        Map map = new HashMap();
        map.put("appId", appId);
        map.put("command", command);
        return map;
    }

}
