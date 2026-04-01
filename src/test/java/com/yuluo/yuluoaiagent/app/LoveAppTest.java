package com.yuluo.yuluoaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是羽洛。";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我有一个暗恋许久的女孩，她叫安和昴，如何慢慢靠近她的心？";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // // 第三轮
        // message = "我喜欢的人叫什么来着，刚刚说过，请你帮我回忆一下";
        // answer = loveApp.doChat(message, chatId);
        // Assertions.assertNotNull(answer);
    }

    /**
     * 测试 Redis 记忆功能 - 使用已有的 chatId 验证记忆读取
     */
    @Test
    void testExistingMemoryRecall() {
        // 使用一个你确认 Redis 中已有记忆的 chatId
        String chatId = "69ff5059-6070-4785-8d54-5e9d08b5460f";
        // 直接询问之前对话中提到的信息
        String message = "你还记得我们之前聊过什么吗？我喜欢的人是谁？";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithSensitiveWords() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是羽洛。我喜欢赌博";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是羽洛，我想让我的暗恋对象安和昴更喜欢我，但我不知道怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }









}
