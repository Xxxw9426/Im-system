package com.lld.im.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.BaseErrorCode;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.GateWayErrorCode;
import com.lld.im.common.exception.ApplicationExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-30
 * @Description: 拦截器
 * @Version: 1.0
 */

@Component
public class GateWayInterceptor implements HandlerInterceptor {

    @Autowired
    IdentityCheck identityCheck;

    // 进行userSign的鉴权
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求参数中的appId，操作人和userSign
        String appIdStr = request.getParameter("appId");
        if(StringUtils.isBlank(appIdStr)){
            resp(ResponseVO.errorResponse(GateWayErrorCode.APPID_NOT_EXIST),response);
            return false;
        }
        // 获取请求参数中的操作人
        String identifier = request.getParameter("identifier");
        if(StringUtils.isBlank(identifier)){
            resp(ResponseVO.errorResponse(GateWayErrorCode.OPERATOR_NOT_EXIST),response);
            return false;
        }

        // 获取请求参数中的userSign
        String userSign = request.getParameter("userSign");
        if(StringUtils.isBlank(userSign)){
            resp(ResponseVO.errorResponse(GateWayErrorCode.USERSIGN_NOT_EXIST),response);
            return false;
        }

        // 校验签名和操作人以及appId是否匹配
        ApplicationExceptionEnum applicationExceptionEnum = identityCheck.checkUserSig(identifier, appIdStr, userSign);
        if(applicationExceptionEnum!= BaseErrorCode.SUCCESS) {
            resp(ResponseVO.errorResponse(applicationExceptionEnum),response);
            return false;
        }
        return true;
    }


    // 向调用方返回错误信息
    private void resp(ResponseVO respVo,HttpServletResponse response) {
        PrintWriter writer = null;
        // 设置字符集
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        // 向调用者返回错误
        try{
            String resp= JSONObject.toJSONString(respVo);
            writer=response.getWriter();
            writer.write(resp);
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            if(writer!=null){
                writer.close();
            }
        }
    }
}
