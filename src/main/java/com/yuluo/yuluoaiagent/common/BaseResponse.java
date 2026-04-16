package com.yuluo.yuluoaiagent.common;

import com.yuluo.yuluoaiagent.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局响应类
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 成功响应
     * @param code 状态码
     * @param data 数据
     */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 错误响应
     * @param errorCode 错误码
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}

