package com.yuluo.yuluoaiagent.tools;

import com.yuluo.yuluoaiagent.app.LoveApp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ToolRegistrationTest {
    @Resource
    private LoveApp loveApp;
    // @Test
    // void doChatWithTools() {
    //     // 测试联网搜索问题的答案
    //     testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");
    //
    //     // 测试网页抓取：恋爱案例分析
    //     testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");
    //
    //     // 测试资源下载：图片下载
    //     testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");
    //
    //     // 测试文件操作：保存用户档案
    //     testMessage("保存我的恋爱档案为文件");
    //
    // }

    // private void testMessage(String message) {
    //     String chatId = UUID.randomUUID().toString();
    //     String answer = loveApp.doChatWithTools(message, chatId);
    //     Assertions.assertNotNull(answer);
    // }

}