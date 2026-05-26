package com.zx.common.cache.exception;

import com.zx.common.base.enums.ErrorCode;
import com.zx.common.base.exception.BaseException;

/**
 * @author ZhaoXu
 * @date 2022/5/15 22:19
 */
public class RedLockException extends BaseException {
    private static final long serialVersionUID = -3284805116892805544L;

    public RedLockException(String message) {
        super(message);
    }

    public RedLockException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
}
