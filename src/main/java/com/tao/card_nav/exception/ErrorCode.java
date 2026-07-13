package com.tao.card_nav.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "ok"),
    PARAMS_ERROR(400, "请求参数错误"),
    CARD_URL_CONFLICT(400, "卡片链接重复"),
    CARD_TITLE_CONFLICT(400, "卡片标题重复"),
    CHALLENGE_INVALID(403, "验证码无效或已过期"),
    NOT_LOGIN_ERROR(401, "未登录"),
    NO_AUTH_ERROR(401, "无权限"),
    TOO_MANY_REQUEST(429, "请求过于频繁"),
    NOT_FOUND_ERROR(404, "请求数据不存在"),
    FORBIDDEN_ERROR(403, "禁止访问"),
    EMAIL_SEND_FAILED(500, "邮件发送失败"),
    SYSTEM_ERROR(500, "系统内部异常"),
    OPERATION_ERROR(501, "操作失败");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}