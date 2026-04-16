package com.yuluo.yuluoaiagent.exception;

import com.yuluo.yuluoaiagent.common.BaseResponse;
import com.yuluo.yuluoaiagent.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
@Slf4j
@Hidden
public class AiGlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> handleAiBusinessException(BusinessException e) {
        log.error("AI 业务异常: code={}, message={}", e.getCode(), e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(TimeoutException.class)
    public BaseResponse<?> handleAiTimeoutException(TimeoutException e) {
        log.error("AI 响应超时", e);
        return ResultUtils.error(ErrorCode.AI_TIMEOUT_ERROR, "AI 响应超时，请稍后重试");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<?> handleAiIllegalArgumentException(IllegalArgumentException e) {
        log.error("AI 参数错误: {}", e.getMessage(), e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> handleAiRuntimeException(RuntimeException e) {
        log.error("AI 运行时异常: {}", e.getMessage(), e);
        String errorMessage = e.getMessage();
        if (errorMessage != null && errorMessage.contains("API key")) {
            return ResultUtils.error(ErrorCode.AI_MODEL_ERROR, "AI 服务配置错误");
        } else if (errorMessage != null && errorMessage.contains("rate limit")) {
            return ResultUtils.error(ErrorCode.AI_MODEL_ERROR, "AI 服务繁忙，请稍后重试");
        }
        return ResultUtils.error(ErrorCode.AI_MODEL_ERROR, "AI 服务异常，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleAiException(Exception e) {
        log.error("AI 未知异常: {}", e.getMessage(), e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "AI 服务出现未知错误");
    }
}
