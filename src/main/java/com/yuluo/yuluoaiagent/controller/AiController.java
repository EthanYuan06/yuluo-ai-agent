package com.yuluo.yuluoaiagent.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yuluo.yuluoaiagent.agent.model.YuluoLoveManus;
import com.yuluo.yuluoaiagent.app.LoveApp;
import com.yuluo.yuluoaiagent.common.BaseResponse;
import com.yuluo.yuluoaiagent.common.ResultUtils;
import com.yuluo.yuluoaiagent.model.dto.LoveReportDTO;
import com.yuluo.yuluoaiagent.exception.BusinessException;
import com.yuluo.yuluoaiagent.exception.ErrorCode;
import com.yuluo.yuluoaiagent.model.vo.LoveReportVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {
    @Resource
    private LoveApp loveApp;
    @Resource
    private ToolCallback[] allTools;
    @Resource
    private ToolCallbackProvider toolCallbackProvider;
    @Resource
    private ChatModel textChatModel;
    @Resource
    private ChatModel visionChatModel;

    /**
     * 对话（流式输出、支持图像理解）
     *
     * @param imageUrl 图片 url
     * @param message  用户消息
     * @param chatId   对话 id
     * @return 流式文本
     */
    @PostMapping("love_app/chat/stream")
    public SseEmitter doChatByStream(@RequestParam(required = false) String imageUrl,
                                     String message,
                                     String chatId) throws Exception {
        // 1. 创建 SSE 连接
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);

        // 2. 流式订阅
        loveApp.doChatByStream(imageUrl, message, chatId).subscribe(
                // 正常消息 - SSE流式响应直接发送文本内容，不包装为BaseResponse
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },

                // 异常处理 - 发送错误事件
                error -> {
                    log.error("流式对话出错", error);
                    String errorMessage;
                    if (error instanceof BusinessException bizEx) {
                        errorMessage = bizEx.getMessage();
                    } else {
                        errorMessage = error.getMessage();
                    }
                    try {
                        // 使用 SSE 事件发送错误信息
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(errorMessage));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                // 正常结束
                emitter::complete
        );
        return emitter;
    }

    /**
     * 推荐恋爱对象
     *
     * @param message 用户消息
     * @param chatId  对话 id
     * @return 流式文本
     */
    @PostMapping("love_app/chat/recommend_lovers")
    public SseEmitter recommendLovers(String message, String chatId) {
        // 1. 创建 SSE 连接
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);

        // 2. 流式订阅
        loveApp.recommendLovers(message, chatId).subscribe(
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
     * 生成恋爱报告（结构化输出）
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return 恋爱报告
     */
    @PostMapping("love_app/report")
    public BaseResponse<LoveReportVO> generateLoveReport(String message, String chatId) {
        LoveReportDTO dto = loveApp.doChatWithReport(message, chatId);
        LoveReportVO vo = new LoveReportVO(dto.title(), dto.suggestions());
        return ResultUtils.success(vo);
    }

    /**
     * 流式调用 AI 智能体
     *
     * @param message 消息
     * @return 响应
     */
    @PostMapping("manus/chat")
    public SseEmitter doChatWithManus(String message) {
        YuluoLoveManus yuluoLoveManus = new YuluoLoveManus(allTools, toolCallbackProvider, textChatModel);
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
