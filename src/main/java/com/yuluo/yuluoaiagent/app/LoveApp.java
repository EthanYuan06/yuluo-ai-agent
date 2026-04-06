package com.yuluo.yuluoaiagent.app;

import com.yuluo.yuluoaiagent.advisor.ForbiddenWordAdvisor;
import com.yuluo.yuluoaiagent.advisor.MyLoggerAdvisor;
import com.yuluo.yuluoaiagent.chatmemory.RedisKryoChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.Media;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient QAchatClient;
    private final ChatClient RecommendChatClient;
    private final ChatClient GenderDetectionChatClient;
    private final VectorStore loveAppVectorStore;
    private final VectorStore candidateVectorStore;
    private final Advisor loveAppRagCloudAdvisor;
    private final String QAPromptContent;

    public LoveApp(ChatModel dashscopeChatModel,
                   RedisKryoChatMemory chatMemory,
                   @Value("classpath:prompt/qa-system-prompt.st")
                   Resource QASystemResource,
                   @Value("classpath:prompt/recommend-system-prompt.st")
                   Resource RecommendSystemResource,
                   VectorStore loveAppVectorStore,
                   VectorStore candidateVectorStore,
                   Advisor loveAppRagCloudAdvisor) {
        // 创建问答客户端
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(QASystemResource);
        this.QAPromptContent = systemPromptTemplate.render(Map.of("name", "千咲", "voice", "温和的"));
        QAchatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(QAPromptContent)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志advisor
                        // new MyLoggerAdvisor(),
                        // 自定义过滤敏感词advisor
                        new ForbiddenWordAdvisor()
                )
                .build();

        // 创建推荐对象客户端
        SystemPromptTemplate recommendSystemPromptTemplate = new SystemPromptTemplate(RecommendSystemResource);
        String RecommendPromptContent = recommendSystemPromptTemplate.render();
        RecommendChatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(RecommendPromptContent)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志advisor
                        new MyLoggerAdvisor(),
                        // 自定义过滤敏感词advisor
                        new ForbiddenWordAdvisor()
                )
                .build();
        // 创建性别检测客户端
        GenderDetectionChatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("你是一个性别意图解析器。分析用户输入，判断用户想推荐的候选人性别。只输出JSON：{\"targetGender\": \"male\"} 或 {\"targetGender\": \"female\"} 或 {\"targetGender\": \"unknown\"}")
                .build();
        this.loveAppVectorStore = loveAppVectorStore;
        this.candidateVectorStore = candidateVectorStore;
        this.loveAppRagCloudAdvisor = loveAppRagCloudAdvisor;
    }

    /**
     * 多轮对话记忆聊天功能
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return 响应内容
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = QAchatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("Response: {}", content);
        return content;
    }

    /**
     * 基于 RAG + 本地知识库回复用户问题
     * @param message 用户提示词
     * @param chatId 会话 ID
     * @return 响应内容
     */
    public String doChatWithRag(String message, String chatId) {
        ChatResponse response = QAchatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                // 检索本地向量数据库
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 检索云知识库
                // .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("Response with RAG: {}", content);
        return content;
    }

    /**
     * 向用户推荐对象
     * @param message 用户提示词
     * @param chatId 会话 ID
     * @return 响应内容
     */
    public String recommendLovers(String message, String chatId) {
        String targetGender = detectTargetGender(message);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)
                .topK(5)
                .filterExpression("gender == '" + targetGender + "'")
                .build();
        List<Document> filteredDocs = candidateVectorStore.similaritySearch(searchRequest);
        log.info("性别过滤条件: gender == '{}', 过滤后剩余片段数: {}", targetGender, filteredDocs.size());
        ChatResponse response = RecommendChatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                // 检索本地向量数据库
                .advisors(new QuestionAnswerAdvisor(candidateVectorStore, searchRequest))
                // 检索云知识库
                // .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("Recommend response: {}", content);
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
        ChatResponse response = QAchatClient
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
        LoveReport loveReport = QAchatClient
                .prompt()
                .system(QAPromptContent + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表，分点给出5条建议。")
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

    /**
     * 检测目标性别
     * @param message 用户输入
     * @return  性别
     */
    private String detectTargetGender(String message) {
        String jsonResponse = GenderDetectionChatClient
                .prompt()
                .user(message)
                .call()
                .content();

        log.info("性别检测原始响应: {}", jsonResponse);

        Pattern pattern = Pattern.compile("\"targetGender\"\\s*:\\s*\"(male|female|unknown)\"");
        Matcher matcher = pattern.matcher(jsonResponse);
        if (matcher.find()) {
            String targetGender = matcher.group(1);
            return "unknown".equals(targetGender) ? null : targetGender;
        }
        return null;
    }
}
