package com.zx.common.rpc.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RpcErrorEnums {
    DOMAIN_CANT_NOT_EMPTY(1000001, "request client domain must not be empty"),
    ANNOTATION_CANT_NOT_EMPTY(1000002, "request mapping annotation must not be empty"),
    CAN_NOT_FIND_REQUEST_PATH(1000003, "request path cannot be found");

    private final Integer code;
    private final String message;
}
