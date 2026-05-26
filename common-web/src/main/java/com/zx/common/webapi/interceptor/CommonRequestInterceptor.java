package com.zx.common.webapi.interceptor;

import com.zx.common.base.enums.ErrorCode;
import com.zx.common.base.model.BaseResponse;
import com.zx.common.base.utils.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

/**
 * @author ZhaoXu
 * @date 2024/2/2 10:16
 */
@Component("commonRequestInterceptor")
@Slf4j
public class CommonRequestInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            String methodName = handlerMethod.getMethod().getName();
            Class<?> methodDeclaringClass = handlerMethod.getMethod().getDeclaringClass();
            String fullMethod = methodDeclaringClass.getName() + "#" + methodName + " ";
            request.setAttribute("fullMethod", fullMethod);
        }
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        long startTime = (long) request.getAttribute("startTime");
        Object fail = request.getAttribute("fail");
        Object fullMethod = request.getAttribute("fullMethod");
        String requestUri = request.getRequestURI();
        long costTime = System.currentTimeMillis() - startTime;

        if (fail instanceof BaseResponse) {
            BaseResponse<?> baseResponse = (BaseResponse<?>) fail;
            if (Objects.equals(response.getStatus(), HttpStatus.INTERNAL_SERVER_ERROR.value())) {
                log.error("request.system.error.method:{}, requestUri:{}, request:{}, response:{}, cost:{}ms", fullMethod, requestUri,
                        JsonUtils.toJson(request.getParameterMap()), JsonUtils.toJson(fail), costTime);
            } else {
                log.warn("request.business.error.method:{}, requestUri:{}, request:{}, response:{}, cost:{}ms", fullMethod, requestUri,
                        JsonUtils.toJson(request.getParameterMap()), JsonUtils.toJson(fail), costTime);
            }
        } else {
            log.debug("request.success.method:{}, requestUri:{}, cost:{}ms", fullMethod, requestUri, costTime);
        }
    }
}
