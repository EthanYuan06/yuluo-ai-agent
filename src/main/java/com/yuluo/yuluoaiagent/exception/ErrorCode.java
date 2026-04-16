package com.yuluo.yuluoaiagent.exception;

import lombok.Getter;

/**
 * 错误码定义
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    AI_MODEL_ERROR(50010, "AI 模型调用失败"),
    AI_TOOL_ERROR(50011, "AI 工具执行失败"),
    AI_RAG_ERROR(50012, "AI 知识检索失败"),
    AI_CONTENT_FILTERED(40301, "内容包含违禁词，已被拦截"),
    AI_TIMEOUT_ERROR(50013, "AI 响应超时");

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
