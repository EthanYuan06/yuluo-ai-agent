package com.yuluo.yuluoaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 配置类：基于本地向量数据库的 RAG 功能
 */
@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    /**
     * 创建向量数据库（恋爱问答）
     *
     * @param dashscopeEmbeddingModel 模型
     * @return 向量存储
     */
    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        List<Document> documents = loveAppDocumentLoader.loadQADocuments();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }
    /**
     * 创建向量数据库（恋爱对象推荐）
     *
     * @param dashscopeEmbeddingModel 模型
     * @return 向量存储
     */
    @Bean
    VectorStore candidateVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        List<Document> candidateDocuments = loveAppDocumentLoader.loadCandidateDocuments();
        simpleVectorStore.add(candidateDocuments);
        return simpleVectorStore;
    }
}
