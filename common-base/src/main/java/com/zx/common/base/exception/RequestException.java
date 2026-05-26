package com.zx.common.base.exception;

import com.zx.common.base.enums.ErrorCode;

/**
 * @author ZhaoXu
 * @date 2023/11/10 14:43
 */
public class RequestException extends BaseException {
    private static final long serialVersionUID = -8048925526606672912L;

    public RequestException(String message) {
        super(message);
    }
}
