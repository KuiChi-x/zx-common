package com.zx.common.webapi.response;

import com.zx.common.base.model.BaseResponse;
import com.zx.common.webapi.annotation.NotRewriteBody;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * @author ZhaoXu
 * @date 2023/10/13 17:47
 */
@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter methodParameter, @Nullable Class<? extends HttpMessageConverter<?>> aClass) {
        Class<?> parameterType = methodParameter.getParameterType();
        boolean unSupport = parameterType.isAssignableFrom(BaseResponse.class)
                || parameterType.isAssignableFrom(ResponseEntity.class)
                || methodParameter.hasMethodAnnotation(NotRewriteBody.class);
        return !unSupport;
    }

    @Override
    public Object beforeBodyWrite(Object data, MethodParameter returnType, @Nullable MediaType mediaType, @Nullable Class<? extends HttpMessageConverter<?>> aClass,
                                  @Nullable ServerHttpRequest request, @Nullable ServerHttpResponse response) {
        return new BaseResponse<>(data);
    }
}