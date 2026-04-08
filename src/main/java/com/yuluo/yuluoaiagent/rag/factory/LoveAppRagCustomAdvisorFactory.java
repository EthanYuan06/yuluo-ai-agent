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
 */
@Slf4j
public class LoveAppRagCustomAdvisorFactory {
    // 1. 问答：按 status 过滤
    public static Advisor createAdvisorForQA(VectorStore vectorStore, String status) {
        Filter.Expression filter = new FilterExpressionBuilder().eq("status", status).build();
        return createRagAdvisor(vectorStore, filter);
    }

    // 2. 推荐：按 gender 过滤
    public static Advisor createAdvisorForRecommend(VectorStore vectorStore, String gender) {
        Filter.Expression filter = new FilterExpressionBuilder().eq("gender", gender).build();
        return createRagAdvisor(vectorStore, filter);
    }

    /**
     * 创建 RAG Advisor
     */
    private static Advisor createRagAdvisor(VectorStore vectorStore, Filter.Expression filterExpression) {
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(filterExpression)
                .similarityThreshold(0.5)
                .topK(3)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(LoveAppContextualQueryAugmenterFactory.createInstance())
                .build();
    }
}
