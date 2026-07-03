package com.tao.card_nav.exception;

import com.tao.card_nav.dict.ResultCodeEnum;
import com.tao.card_nav.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        e.printStackTrace();
        return Result.error(ResultCodeEnum.INTERNAL_ERROR.getCode(), "服务器内部错误: " + e.getMessage());
    }
}
