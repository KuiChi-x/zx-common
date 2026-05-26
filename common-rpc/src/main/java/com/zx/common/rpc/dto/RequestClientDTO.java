package com.zx.common.rpc.dto;

import com.fasterxml.jackson.databind.JavaType;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.Serializable;
import java.util.Map;

/**
 * @author ZhaoXu
 * @date 2023/11/8 13:29
 */
@Data
public class RequestClientDTO implements Serializable {
    private static final long serialVersionUID = 4693250676777612892L;

    /**
     * 请求域名
     */
    String domain;

    /**
     * 请求头
     */
    Map<String, Object> headers;

    /**
     * 请求方式
     * @see RequestMethod
     */
    RequestMethod requestMethod;

    /**
     * 请求路径
     */
    String path;

    /**
     * 请求体
     */
    Object requestBody;

    JavaType responseJavaType;
}
