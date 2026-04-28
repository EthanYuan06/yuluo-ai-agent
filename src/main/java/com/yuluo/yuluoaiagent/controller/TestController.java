package com.yuluo.yuluoaiagent.controller;

import com.yuluo.yuluoaiagent.advisor.MyLoggerAdvisor;
import com.yuluo.yuluoaiagent.app.LoveApp;
import com.yuluo.yuluoaiagent.common.BaseResponse;
import com.yuluo.yuluoaiagent.common.ResultUtils;
import com.yuluo.yuluoaiagent.exception.BusinessException;
import com.yuluo.yuluoaiagent.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * 测试控制器 - 用于测试各种功能
 */
@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @Resource
    private LoveApp loveApp;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ToolCallbackProvider toolCallbackProvider;
    @Resource
    private ChatModel textChatModel;

    // /**
    //  * 测试 Redis + Kryo 单次会话存储大小
    //  *
    //  * @param message 用户输入消息
    //  * @param chatId  会话ID（可选，默认 test-session）
    //  * @return 存储大小信息
    //  */
    // @GetMapping("memory-size")
    // public BaseResponse<String> testMemorySize(String message, String chatId) {
    //     if (chatId == null || chatId.isEmpty()) {
    //         chatId = "test-session";
    //     }
    //
    //     // 1. 执行一次对话（会触发 Redis 存储）
    //     loveApp.doChatByStream(message != null ? message : "你好", chatId);
    //
    //     // 2. 从 Redis 获取原始数据大小
    //     String key = "chat:memory:" + chatId;
    //     String dataStr = stringRedisTemplate.opsForValue().get(key);
    //
    //     if (dataStr == null) {
    //         throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在");
    //     }
    //
    //     // 3. 计算大小
    //     byte[] rawBytes = dataStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    //     byte[] decodedBytes = java.util.Base64.getDecoder().decode(dataStr);
    //
    //     int base64Size = rawBytes.length;
    //     int kryoSize = decodedBytes.length;
    //
    //     String result = String.format(
    //             "会话ID: %s\n" +
    //                     "【实际存储】Base64编码后大小: %d bytes (%.2f KB) ← Redis 实际占用\n" +
    //                     "【参考】Kryo序列化原始大小: %d bytes (%.2f KB)\n" +
    //                     "Redis存储键: %s",
    //             chatId,
    //             base64Size, base64Size / 1024.0,
    //             kryoSize, kryoSize / 1024.0,
    //             key
    //     );
    //
    //     return ResultUtils.success(result);
    // }

    /**
     * 测试 MCP 对话功能（流式版本 - 推荐用于复杂任务）
     * 使用 MCP Server 提供的工具进行智能对话
     *
     * @param message 用户输入消息
     * @return SSE 流式响应
     */
    @GetMapping("/mcp-chat-stream")
    public SseEmitter testMcpChatStream(String message) {
        log.info("开始 MCP 流式对话，message: {}", message);
        
        // 创建 5 分钟超时的 SSE 连接（MCP操作可能耗时较长）
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        
        ChatClient mcpChatClient = ChatClient.builder(textChatModel).build();
        
        // 异步执行 MCP 调用
        Flux<String> responseFlux = mcpChatClient
                .prompt()
                .user(message)
                .tools(toolCallbackProvider)
                .stream()
                .content();
        
        responseFlux.subscribe(
                chunk -> {
                    try {
                        // SSE流式响应直接发送文本内容，不包装为BaseResponse
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (Exception e) {
                        log.error("发送 SSE 消息失败", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("MCP 流式对话失败", error);
                    try {
                        // SSE错误处理也发送纯文本，避免Content-Type冲突
                        String errorMsg = "MCP 调用失败: " + error.getMessage();
                        emitter.send(SseEmitter.event().data(errorMsg));
                    } catch (IOException e) {
                        log.error("发送错误消息失败", e);
                    } finally {
                        emitter.completeWithError(error);
                    }
                },
                () -> {
                    log.info("MCP 流式对话完成");
                    emitter.complete();
                }
        );
        
        return emitter;
    }

    /**
     * 测试 MCP 对话功能（同步版本 - 简单任务使用）
     * 使用 MCP Server 提供的工具进行智能对话
     *
     * @param message 用户输入消息
     * @return AI 响应内容
     */
    @GetMapping("/mcp-chat")
    public BaseResponse<String> testMcpChat(String message) {
        ChatClient mcpChatClient = ChatClient.builder(textChatModel).build();

        try {
            log.info("开始 MCP 对话，message: {}", message);
            String response = mcpChatClient
                    .prompt()
                    .user(message)
                    .tools(toolCallbackProvider)
                    .call()
                    .content();

            log.info("MCP 对话完成，response: {}", response);
            return ResultUtils.success(response);
        } catch (Exception e) {
            log.error("MCP 对话失败", e);
            throw new BusinessException(ErrorCode.AI_MODEL_ERROR, 
                    "MCP 调用失败: " + e.getMessage());
        }
    }
}
