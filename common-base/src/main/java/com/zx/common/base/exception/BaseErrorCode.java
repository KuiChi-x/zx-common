package com.zx.common.base.exception;

/**
 * @author ZhaoXu
 * @date 2024/2/1 19:45
 */
public interface BaseErrorCode {
    /**
     * 错误码
     * @return
     */
    int getCode();

    /**
     * 错误信息
     * @return
     */
    String getMessage();
}
