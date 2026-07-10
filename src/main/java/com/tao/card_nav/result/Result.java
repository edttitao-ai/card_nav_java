package com.tao.card_nav.result;

import com.tao.card_nav.exception.ErrorCode;
import lombok.Data;

/**
 * 统一响应包装。
 *
 * <p>状态码 / 信息全部以 {@link ErrorCode} 为唯一来源。
 * 不再引用 {@code ResultCodeEnum}（已删除）。
 */
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMessage(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 错误响应（首选）。所有 controller / 异常处理器走这个。
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMessage(errorCode.getMessage());
        return result;
    }

    /**
     * 错误响应（带自定义 message）。
     */
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMessage(message);
        return result;
    }

    /**
     * 兜底错误。仅用于"没有合适 ErrorCode 时"的临时出口；新代码应优先 {@link #error(ErrorCode)}。
     */
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 兜底错误（仅 message）。走 {@link ErrorCode#SYSTEM_ERROR}。
     */
    public static <T> Result<T> error(String message) {
        return error(ErrorCode.SYSTEM_ERROR, message);
    }
}
