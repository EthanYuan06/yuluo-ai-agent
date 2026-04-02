package com.yuluo.yuluoaiagent.app;

import com.yuluo.yuluoaiagent.advisor.ForbiddenWordAdvisor;
import com.yuluo.yuluoaiagent.chatmemory.RedisKryoChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private final String systemPromptContent;

    public LoveApp(ChatModel dashscopeChatModel,
                   RedisKryoChatMemory chatMemory,
                   @Value("classpath:prompt/system-prompt.st")
                   Resource systemResource) {
        // 直接使用资源创建模板
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        this.systemPromptContent = systemPromptTemplate.render(Map.of("name", "千咲", "voice", "温和的"));
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPromptContent)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志advisor
                        // new MyLoggerAdvisor(),
                        // 自定义过滤敏感词advisor
                        new ForbiddenWordAdvisor()
                )
                .build();
    }

    // /**
    //  * 构造函数，初始化聊天客户端
    //  * 基于内存的对话记忆
    //  * @param dashscopeChatModel 模型
    //  */
    // public LoveApp(ChatModel dashscopeChatModel,
    //                @Value("classpath:prompt/system-prompt.st")
    //                Resource systemResource){
    //     // 直接使用资源创建模板
    //     SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
    //     this.systemPromptContent = systemPromptTemplate.render(Map.of("name", "安和昴", "voice", "温和的"));
    //     // 初始化基于内存的对话记忆
    //     ChatMemory chatMemory = new InMemoryChatMemory();
    //     chatClient = ChatClient.builder(dashscopeChatModel)
    //             .defaultSystem(systemPromptContent)
    //             .defaultAdvisors(
    //                     new MessageChatMemoryAdvisor(chatMemory),
    //                     // 自定义日志advisor
    //                     // new MyLoggerAdvisor(),
    //                     // 自定义过滤敏感词advisor
    //                     new ForbiddenWordAdvisor()
    //             )
    //             .build();
    // }

    /**
     * 多轮对话记忆聊天功能
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return 响应内容
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 图像理解功能（多模态）
     *
     * @param imageUrl 图片 URL
     * @param message 用户提示词
     * @param chatId 会话 ID
     * @return 响应内容
     */
    public String analyzeImage(String imageUrl, String message, String chatId) throws Exception {
        // 将图片 URL 转换为 Resource，让Spring可以识别
        UrlResource urlResource = new UrlResource(imageUrl);
        // 构建图片媒体对象，将图片封装为AI能识别的资源
        Media media = Media.builder()
                // 声明图片类型
                .mimeType(MimeTypeUtils.IMAGE_JPEG)
                // 绑定资源
                .data(urlResource)
                .build();
        ChatResponse response = chatClient
                .prompt()
                .user(userSpec -> userSpec
                        // 绑定文本指令
                        .text(message)
                        // 绑定图片媒体资源
                        .media(media))
                .advisors(spec -> spec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("analyzeImage: {}", content);
        return content;
    }


    /**
     * 恋爱报告生成（结构化输出）
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return 响应内容
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(systemPromptContent + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表，分点给出5条建议。")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    // 恋爱报告类（静态成员类）
    public record LoveReport(String title, List<String> suggestions) {
    }
}
