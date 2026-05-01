package com.yuluo.yuluoaiagent.rag.factory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 自定义 RAG 检索增强 advisor
 * 根据登录用户的基本信息传递过滤条件
 */
@Slf4j
public class LoveAppRagCustomAdvisorFactory {
    /**
     * 创建通用Advisor（不过滤，检索所有文档）
     */
    public static Advisor createGenericAdvisor(VectorStore vectorStore) {
        return createRagAdvisor(vectorStore, null);
    }

    /**
     * 按文档类型过滤
     * @param docType 文档类型: "qa", "candidate", "general"
     */
    public static Advisor createAdvisorByDocType(VectorStore vectorStore, String docType) {
        Filter.Expression filter = new FilterExpressionBuilder()
                .eq("docType", docType)
                .build();
        return createRagAdvisor(vectorStore, filter);
    }

    /**
     * 按性别过滤（用于推荐场景）
     * @param gender 性别: "female", "male"
     */
    public static Advisor createAdvisorByGender(VectorStore vectorStore, String gender) {
        Filter.Expression filter = new FilterExpressionBuilder()
                .eq("gender", gender)
                .build();
        return createRagAdvisor(vectorStore, filter);
    }

    /**
     * 按个人状态过滤
     * @param status 状态标识
     */
    public static Advisor createAdvisorByStatus(VectorStore vectorStore, String status) {
        Filter.Expression filter = new FilterExpressionBuilder()
                .eq("status", status)
                .build();
        return createRagAdvisor(vectorStore, filter);
    }

    /**
     * 创建 RAG Advisor
     * 相似度阈值 = 0.5，topK = 5
     */
    private static Advisor createRagAdvisor(VectorStore vectorStore, Filter.Expression filterExpression) {
        // 文档增强配置
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(filterExpression)
                .similarityThreshold(0.5)
                .topK(5)
                .build();
        // 构建RAG Advisor
        return RetrievalAugmentationAdvisor.builder()
                // 加载配置好的文档增强配置
                .documentRetriever(documentRetriever)
                // 无法回答时触发空上下文，使用特定话术回复用户
                .queryAugmenter(LoveAppContextualQueryAugmenterFactory.createInstance())
                .build();
    }
}
