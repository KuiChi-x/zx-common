package com.zx.common.base.enums;

import com.zx.common.base.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ZhaoXu
 * @date 2022/6/7 16:34
 */
@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseErrorCode {
    /**
     * 错误码
     */
    SYSTEM_EXCEPTION(-1, "SYSTEM_EXCEPTION"),
    SUCCESS(0, "SUCCESS"),
    BUSINESS_EXCEPTION(2000000, "BUSINESS_EXCEPTION"),
    BAD_PARAMS(2000001, "BAD_PARAMS"),
    CAN_NOT_FIND_NODE(2000003, "未找到redis节点！"),
    REQUEST_ERROR(2000004, "请求出错！"),
    ;

    private final int code;
    private final String message;

}
