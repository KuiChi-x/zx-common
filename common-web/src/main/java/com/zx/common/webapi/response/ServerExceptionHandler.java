package com.zx.common.webapi.response;

import com.zx.common.base.enums.ErrorCode;
import com.zx.common.base.exception.BaseException;
import com.zx.common.base.model.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ZhaoXu
 * @date 2023/10/13 16:47
 */
@RestControllerAdvice
@Slf4j
public class ServerExceptionHandler {
    @ExceptionHandler({Throwable.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<Object> throwable(Throwable e, WebRequest request) {
        BaseResponse<Object> fail = BaseResponse.fail(Optional.ofNullable(e.getMessage()).orElse(ErrorCode.SYSTEM_EXCEPTION.getMessage()),
                ErrorCode.SYSTEM_EXCEPTION.getCode());
        request.setAttribute("fail", fail, WebRequest.SCOPE_REQUEST);
        log.error("发生系统错误:{}", e.getMessage(), e);
        return fail;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.OK)
    public BaseResponse<Object> handleMethodArgumentsNotValid(MethodArgumentNotValidException e, WebRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessages = bindingResult.getAllErrors().stream()
                .map(item -> (ObjectUtils.isEmpty(item.getCodes()) ? "" : item.getCodes()[0]) + item.getDefaultMessage())
                .collect(Collectors.joining(" & "));
        String message = "参数校验失败: " + errorMessages;
        BaseResponse<Object> fail = BaseResponse.fail(message, ErrorCode.BAD_PARAMS.getCode());
        request.setAttribute("fail", fail, WebRequest.SCOPE_REQUEST);
        return fail;
    }

    @ExceptionHandler(BaseException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public BaseResponse<Object> baseException(BaseException e, WebRequest request) {
        BaseResponse<Object> fail = BaseResponse.fail(e.getMessage(), Optional.ofNullable(e.getCode())
                .orElse(ErrorCode.BUSINESS_EXCEPTION.getCode()));
        request.setAttribute("fail", fail, WebRequest.SCOPE_REQUEST);
        log.warn("发生业务错误:{}", e.getMessage(), e);
        return fail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public BaseResponse<Object> invalidFormatException(MethodArgumentTypeMismatchException e, WebRequest request) {
        BaseResponse<Object> fail = BaseResponse.fail("参数错误，请检查！", ErrorCode.BAD_PARAMS.getCode());
        request.setAttribute("fail", fail, WebRequest.SCOPE_REQUEST);
        return fail;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public BaseResponse<Object> missingServletRequestParameterException(MissingServletRequestParameterException e, WebRequest request) {
        String parameterName = e.getParameterName();
        BaseResponse<Object> fail = BaseResponse.fail("请求参数 " + parameterName + " 不存在!", ErrorCode.BAD_PARAMS.getCode());
        request.setAttribute("fail", fail, WebRequest.SCOPE_REQUEST);
        return fail;
    }
}
