package com.tao.card_nav.exception;

import com.tao.card_nav.dict.ResultCodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCodeEnum.BAD_REQUEST.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
