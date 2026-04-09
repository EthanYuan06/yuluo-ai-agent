package com.yuluo.yuluoaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;


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


    @Test
    void analyzeImage() throws Exception {
        String chatId = UUID.randomUUID().toString();
        // 使用在线图片 URL
        String imageUrl = "https://i0.hdslb.com/bfs/new_dyn/d836ce1445f7c00025c75776fc4631f01030662977.jpg";
        String message = "图中的女孩在干什么？";

        String answer = loveApp.analyzeImage(imageUrl, message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertFalse(answer.isEmpty());
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我现在是已婚状态，但我跟妻子的关系目前不太亲密，怎么办？";
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void recommendFemaleLovers() {
        String chatId = UUID.randomUUID().toString();
        String message = "我是一名刚刚实习的程序员男大学生，今年22岁，目前单身，喜欢上进、温柔、体贴的女生。" +
                "我平常喜欢讨论关于自我成长的话题，喜欢旅行、看风景。" +
                "请帮我推荐一名女生，与我的年龄相差不超过5岁。";
        String answer = loveApp.recommendLovers(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void recommendMaleLovers() {
        String chatId = UUID.randomUUID().toString();
        String message = "我是22岁的程序员女生！刚实习超有干劲，目前单身，喜欢温柔靠谱、一起进步的男生~" +
                "平时爱聊成长、爱旅行看风景，想找个同频的搭子！" +
                "帮我推荐一位年龄差不超过5岁的男生吧！";
        String answer = loveApp.recommendLovers(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
