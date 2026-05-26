package com.zx.common.rpc.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ZhaoXu
 * @date 2023/11/8 14:48
 */
@Getter
@AllArgsConstructor
public enum RpcErrorEnums {
    /**
     * rpc调用错误信息
     */
    DOMAIN_CANT_NOT_EMPTY(1000001, "domain不可以为空"),

    ANNOTATION_CANT_NOT_EMPTY(1000002, "Annotation不可以为空"),

    CAN_NOT_FIND_REQUEST_PATH(1000003, "找不到请求路径"),

    REQUEST_ERROR(1000004, "请求出错"),
    ;
    private final Integer code;
    private final String message;
}
