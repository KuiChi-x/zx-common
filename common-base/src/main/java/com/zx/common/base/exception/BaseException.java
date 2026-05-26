package com.zx.common.base.exception;

import com.zx.common.base.enums.ErrorCode;
import lombok.Data;

import java.text.MessageFormat;

/**
 * @author ZhaoXu
 * @date 2022/6/3 23:18
 */
@Data
public class BaseException extends RuntimeException {
    private static final long serialVersionUID = -1818626802303482513L;

    private Integer code;

    public BaseException() {
        super();
    }

    public BaseException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_EXCEPTION.getCode();
    }

    public BaseException(String message, Throwable throwable) {
        super(message, throwable);
        this.code = ErrorCode.BUSINESS_EXCEPTION.getCode();
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(Integer code, String message, Throwable throwable) {
        super(message, throwable);
        this.code = code;
    }

    public BaseException(Integer code, String message, Object... args) {
        super(MessageFormat.format(message, args));
        this.code = code;
    }

    public BaseException(Integer code, String message, Throwable throwable, Object... args) {
        super(MessageFormat.format(message, args), throwable);
        this.code = code;
    }

    public BaseException(BaseErrorCode baseErrorCode) {
        super(baseErrorCode.getMessage());
        this.code = baseErrorCode.getCode();
    }

    public BaseException(BaseErrorCode baseErrorCode, Throwable throwable) {
        super(baseErrorCode.getMessage(), throwable);
        this.code = baseErrorCode.getCode();
    }

    public BaseException(BaseErrorCode baseErrorCode, Object... args) {
        super(MessageFormat.format(baseErrorCode.getMessage(), args));
        this.code = baseErrorCode.getCode();
    }

    public BaseException(BaseErrorCode baseErrorCode, Throwable throwable, Object... args) {
        super(MessageFormat.format(baseErrorCode.getMessage(), args), throwable);
        this.code = baseErrorCode.getCode();
    }

    @Override
    public String toString() {
        String string = "BaseException(code={0}, message={1})";
        return MessageFormat.format(string, this.getCode(), this.getMessage());
    }
}
