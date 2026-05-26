package com.zx.common.rpc.executor;

import com.zx.common.rpc.util.HttpClientUtil;
import com.zx.common.rpc.dto.RequestClientDTO;
import com.fasterxml.jackson.databind.JavaType;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author ZhaoXu
 * @date 2023/11/8 14:31
 */
@Slf4j
public class HttpExecutor implements BaseExecutor {
    @Override
    public Object execute(RequestClientDTO requestClientDTO) {
        String url = requestClientDTO.getDomain() + "/" + requestClientDTO.getPath();
        Map<String, Object> headers = requestClientDTO.getHeaders();
        Object requestBody = requestClientDTO.getRequestBody();
        // 发送请求
        Object result;
        JavaType responseJavaType = requestClientDTO.getResponseJavaType();
        switch (requestClientDTO.getRequestMethod()) {
            case GET:
                result = HttpClientUtil.get(headers, url, responseJavaType);
                break;
            case POST:
                result = HttpClientUtil.post(headers, url, requestBody, responseJavaType);
                break;
            case PUT:
                result = HttpClientUtil.put(headers, url, requestBody, responseJavaType);
                break;
            case DELETE:
                result = HttpClientUtil.delete(headers, url, requestBody, responseJavaType);
                break;
            default:
                return null;
        }
        return result;
    }
}
