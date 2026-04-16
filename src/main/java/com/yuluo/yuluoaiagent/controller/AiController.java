package com.yuluo.yuluoaiagent.controller;

import com.yuluo.yuluoaiagent.agent.model.YuluoLoveManus;
import com.yuluo.yuluoaiagent.app.LoveApp;
import com.yuluo.yuluoaiagent.common.BaseResponse;
import com.yuluo.yuluoaiagent.common.ResultUtils;
import com.yuluo.yuluoaiagent.exception.BusinessException;
import com.yuluo.yuluoaiagent.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {
    @Resource
    private LoveApp loveApp;
    @Resource
    private ToolCallback[] allTools;
    @Resource
    private ChatModel dashscopeChatModel;

    @GetMapping("love_app/chat/stream")
    public SseEmitter doChatByStream(String message, String chatId) {
        // 1. 创建 SSE 连接
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);

        // 2. 流式订阅
        loveApp.doChatByStream(message, chatId).subscribe(
                // 正常消息（统一包装）
                chunk -> send(emitter, ResultUtils.success(chunk)),

                // 异常处理（统一包装）
                error -> {
                    BaseResponse<?> response;
                    if (error instanceof BusinessException bizEx) {
                        response = ResultUtils.error(bizEx.getCode(), bizEx.getMessage());
                    } else {
                        response = ResultUtils.error(ErrorCode.AI_MODEL_ERROR, error.getMessage());
                    }
                    send(emitter, response);
                    emitter.completeWithError(error);
                },
                // 正常结束
                emitter::complete
        );
        return emitter;
    }

    /**
     * 流式调用 AI 智能体
     *
     * @param message 消息
     * @return 响应
     */
    @GetMapping("manus/chat")
    public SseEmitter doChatWithManus(String message) {
        YuluoLoveManus yuluoLoveManus = new YuluoLoveManus(allTools, dashscopeChatModel);
        return yuluoLoveManus.runStream(message);
    }

    /**
     * 统一发送消息
     */
    private void send(SseEmitter emitter, BaseResponse<?> response) {
        try {
            emitter.send(response, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
