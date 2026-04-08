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
    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore, String status) {
        // 定义过滤表达式
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("status", status)
                .build();
        // 根据以下的配置从向量存储中检索文档
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression) // 过滤条件
                .similarityThreshold(0.5) // 相似度阈值
                .topK(3) // 返回文档数量
                .build();
        // 创建advisor
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                // 使用自定义的上下文查询增强器
                .queryAugmenter(
                        LoveAppContextualQueryAugmenterFactory.createInstance()
                )
                .build();
    }
}
