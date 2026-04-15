package com.yuluo.yuluoaiagent.controller;

import com.yuluo.yuluoaiagent.agent.model.YuluoLoveManus;
import com.yuluo.yuluoaiagent.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
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
        // 创建3分钟超时的emitter
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);
        // 获取flux流并订阅
        loveApp.doChatByStream(message, chatId).subscribe(chunk -> {
            // 处理每条消息
            try {
                // 推送消息
                emitter.send(chunk);
            } catch (IOException e) {
                // 提示错误中断
                emitter.completeWithError(e);
            }
        }, emitter::completeWithError, emitter::complete);
        return emitter;
    }

    /**
     * 流式调用 AI 智能体
     * @param message 消息
     * @return 响应
     */
    @GetMapping("manus/chat")
    public SseEmitter doChatWithManus(String message) {
        YuluoLoveManus yuluoLoveManus = new YuluoLoveManus(allTools, dashscopeChatModel);
        return yuluoLoveManus.runStream(message);
    }

}
