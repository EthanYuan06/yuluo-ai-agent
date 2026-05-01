package com.yuluo.yuluoaiagent.app;

import cn.hutool.core.util.ObjectUtil;
import com.yuluo.yuluoaiagent.advisor.ForbiddenWordAdvisor;
import com.yuluo.yuluoaiagent.advisor.MyLoggerAdvisor;
import com.yuluo.yuluoaiagent.chatmemory.RedisKryoChatMemory;
import com.yuluo.yuluoaiagent.model.dto.LoveReportDTO;
import com.yuluo.yuluoaiagent.rag.QueryRewriter;
import com.yuluo.yuluoaiagent.rag.factory.LoveAppRagCustomAdvisorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.Media;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient QAchatClient;
    private final ChatClient RecommendChatClient;
    private final ChatClient visionChatClient;
    private final ChatClient intentChatClient;
    private final String QAPromptContent;
    @Resource
    private VectorStore pgVectorStore;

    public LoveApp(
            @Qualifier("textChatModel")
            ChatModel dashscopeChatModel,
            @Qualifier("visionChatModel")
            ChatModel visionChatModel,
            RedisKryoChatMemory chatMemory,
            @Value("classpath:prompt/qa-system-prompt.st")
            org.springframework.core.io.Resource QASystemResource,
            @Value("classpath:prompt/recommend-system-prompt.st")
            org.springframework.core.io.Resource RecommendSystemResource,
            @Value("classpath:prompt/intent-system-prompt.st")
            org.springframework.core.io.Resource IntentSystemResource
    ) {

        // 创建问答客户端
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(QASystemResource);
        this.QAPromptContent = systemPromptTemplate
                .render(Map.of("name", "芊芊", "voice", "温和的"));
        QAchatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(QAPromptContent)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义过滤敏感词advisor
                        new ForbiddenWordAdvisor()
                )
                .build();

        // 创建推荐对象客户端
        SystemPromptTemplate recommendSystemPromptTemplate =
                new SystemPromptTemplate(RecommendSystemResource);
        String RecommendPromptContent = recommendSystemPromptTemplate.render();
        RecommendChatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(RecommendPromptContent)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义过滤敏感词advisor
                        new ForbiddenWordAdvisor()
                )
                .build();

        // 创建视觉理解客户端
        visionChatClient = ChatClient.builder(visionChatModel)
                .defaultSystem(QAPromptContent)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new ForbiddenWordAdvisor()
                )
                .build();

        SystemPromptTemplate intentPromptTemplate = new SystemPromptTemplate(IntentSystemResource);
        String intentPromptContent = intentPromptTemplate.render();
        // 创建意图理解客户端
        intentChatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(intentPromptContent)
                .defaultAdvisors(
                        new ForbiddenWordAdvisor()
                )
                .build();
    }

    /**
     * 对话（流式输出）
     * 基于 RAG + 云知识库 + 图片理解 + 工具调用 + MCP
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return 响应内容
     */
    public Flux<String> doChatByStream(String imageUrl, String message, String chatId) throws Exception {
        // 提前判断是否需要 RAG
        boolean needRag = isNeedRag(message);
        log.info("RAG 判断结果: {}, 消息: {}", needRag, message);
        Flux<String> content;
        // 有图片url就解析后回答，没有就直接输出回答内容
        if (ObjectUtil.isNotNull(imageUrl) && !imageUrl.isEmpty()) {
            log.info("检测到图片url，开始解析图片并回答");
            // 将图片 URL 转换为 Resource，让Spring可以识别
            UrlResource urlResource = new UrlResource(imageUrl);
            // 构建图片媒体对象，将图片封装为AI能识别的资源
            Media media = Media.builder()
                    // 声明图片类型
                    .mimeType(MimeTypeUtils.IMAGE_JPEG)
                    // 绑定资源
                    .data(urlResource)
                    .build();
            // 只返回AI输出的文本，支持图片理解
            content = visionChatClient
                    .prompt()
                    .user(userSpec -> userSpec
                            // 绑定文本指令
                            .text(message)
                            // 绑定图片媒体资源
                            .media(media))
                    // 条件启用 RAG
                    .advisors(spec -> {
                                if (needRag) {
                                    spec.advisors(LoveAppRagCustomAdvisorFactory
                                            .createAdvisorByDocType(pgVectorStore, "qa"));
                                }
                            }
                    )
                    // 日志记录
                    .advisors(new MyLoggerAdvisor())
                    // 聊天记忆
                    .advisors(spec -> spec
                            .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                            .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                    .stream()
                    .content();
            ;
        } else {
            content = QAchatClient
                    .prompt()
                    .user(message)
                    // 条件启用 RAG
                    .advisors(spec -> {
                                if (needRag) {
                                    spec.advisors(LoveAppRagCustomAdvisorFactory
                                            .createAdvisorByDocType(pgVectorStore, "qa"));
                                }
                            }
                    )
                    // 日志记录
                    .advisors(new MyLoggerAdvisor())
                    // 聊天记忆
                    .advisors(spec -> spec
                            .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                            .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                    .stream()
                    .content();
            ;
        }
        return content;
    }

    /**
     * 向用户推荐对象
     *
     * @param message 用户提示词
     * @param chatId  会话 ID
     * @return 响应内容
     */
    public Flux<String> recommendLovers(String message, String chatId) {
        // todo 从用户登录的个人信息中获取性别信息，目前先用占位符
        String gender = "female";
        return RecommendChatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                // 启用本地向量数据库
                .advisors(LoveAppRagCustomAdvisorFactory.createAdvisorByGender(pgVectorStore, gender))
                .stream()
                .content();
    }

    /**
     * 恋爱报告生成（结构化输出）
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return 响应内容
     */
    public LoveReportDTO doChatWithReport(String message, String chatId) {
        LoveReportDTO loveReport = QAchatClient
                .prompt()
                .system(QAPromptContent +
                        "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表，分点给出5条建议。")
                .user(message)
                .advisors(spec -> spec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReportDTO.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    /**
     * 根据用户意图判断是否需要 RAG
     *
     * @param message 用户输入
     * @return 判断结果
     */
    private boolean isNeedRag(String message) {
        if (ObjectUtil.isEmpty(message)) {
            return false;
        }

        log.info("意图判断，用户消息：{}", message);
        String result = intentChatClient.prompt()
                .user(message)
                .call()
                .content()
                .trim().toUpperCase();

        log.info("意图判断结果：{}", result);
        return "RAG".equals(result);
    }
}
