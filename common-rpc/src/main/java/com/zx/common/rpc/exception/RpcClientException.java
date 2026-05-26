package com.zx.common.rpc.exception;

import com.zx.common.base.exception.BaseException;

/**
 * @author ZhaoXu
 * @date 2023/11/8 14:53
 */
public class RpcClientException extends BaseException {
    private static final long serialVersionUID = 561358844967514341L;

    public RpcClientException(RpcErrorEnums errorEnum) {
        super(errorEnum.getCode(), errorEnum.getMessage());
    }
}
