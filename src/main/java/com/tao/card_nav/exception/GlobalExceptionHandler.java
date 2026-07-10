package com.tao.card_nav.exception;

import com.tao.card_nav.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        // 生产环境不直接把 e.getMessage() 透出，避免泄露内部信息
        log.error("未捕获异常", e);
        return Result.error(ErrorCode.SYSTEM_ERROR, "服务器内部错误");
    }
}
