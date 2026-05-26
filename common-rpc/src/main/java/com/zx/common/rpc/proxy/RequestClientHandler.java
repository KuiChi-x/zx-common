package com.zx.common.rpc.proxy;

import com.zx.common.base.utils.JsonUtils;
import com.zx.common.base.utils.ReflectUtils;
import com.zx.common.base.utils.SpringManager;
import com.zx.common.rpc.annotation.RequestClient;
import com.zx.common.rpc.config.RequestConfig;
import com.zx.common.rpc.dto.RequestClientDTO;
import com.zx.common.rpc.exception.RpcClientException;
import com.zx.common.rpc.exception.RpcErrorEnums;
import com.zx.common.rpc.executor.HttpExecutor;
import com.zx.common.rpc.loadblance.LoadBalancer;
import com.zx.common.rpc.parameter.ParameterParser;
import com.fasterxml.jackson.databind.JavaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author ZhaoXu
 */
@Slf4j
public class RequestClientHandler<T> implements InvocationHandler {
    private final Class<T> mapperInterface;

    private static final Long LOG_MAX_LENGTH = 1024 * 50L;

    public RequestClientHandler(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        RequestClient requestClient = mapperInterface.getAnnotation(RequestClient.class);
        String[] domains = requestClient.domains();
        if (ObjectUtils.isEmpty(domains)) {
            throw new RpcClientException(RpcErrorEnums.DOMAIN_CANT_NOT_EMPTY);
        }
        RequestClientDTO requestClientDTO = new RequestClientDTO();
        Annotation[] annotations = method.getAnnotations();
        if (ObjectUtils.isEmpty(annotations)) {
            throw new RpcClientException(RpcErrorEnums.ANNOTATION_CANT_NOT_EMPTY);
        }
        // 接口请求路径
        String[] path = ReflectUtils.getAnnotationFieldValue(annotations[0], "value");
        if (ObjectUtils.isEmpty(path)) {
            throw new RpcClientException(RpcErrorEnums.CAN_NOT_FIND_REQUEST_PATH);
        }
        requestClientDTO.setPath(path[0]);
        RequestMethod[] requestMethod = annotations[0].annotationType().getAnnotation(RequestMapping.class).method();
        requestClientDTO.setRequestMethod(requestMethod[0]);

        // 设置请求参数
        ParameterParser.setParameters(method, args, requestClientDTO);

        // 负载均衡域名
        String domain = LoadBalancer.getDomain(mapperInterface.getName(), domains);
        requestClientDTO.setDomain(domain);

        // 请求前配置
        Class<RequestConfig> config = (Class<RequestConfig>) requestClient.config();
        RequestConfig requestConfig = SpringManager.getBean(config);
        requestConfig.invoke(requestClientDTO);

        // 执行请求
        String fullMethodName = method.getDeclaringClass().getName() + "#" + method.getName();
        try {
            // if (ObjectSizeCalculator.getObjectSize(requestClientDTO.getRequestBody()) <= LOG_MAX_LENGTH) {
            //     log.info(fullMethodName + " request:{}", JsonUtils.toJson(requestClientDTO));
            // }

            JavaType javaType = JsonUtils.constructType(method.getGenericReturnType());
            requestClientDTO.setResponseJavaType(javaType);
            Object response = new HttpExecutor().execute(requestClientDTO);
            // if (ObjectSizeCalculator.getObjectSize(requestClientDTO.getRequestBody()) <= LOG_MAX_LENGTH
            //         && ObjectSizeCalculator.getObjectSize(response) <= LOG_MAX_LENGTH) {
            //     log.info(fullMethodName + " success, request:{}, response:{}", JsonUtils.toJson(requestClientDTO), JsonUtils.toJson(response));
            // }

            return response;
        } catch (Exception e) {
            log.warn(fullMethodName + " error, request:{}", JsonUtils.toJson(requestClientDTO), e);
            throw new RpcClientException(RpcErrorEnums.REQUEST_ERROR);
        }
    }
}
