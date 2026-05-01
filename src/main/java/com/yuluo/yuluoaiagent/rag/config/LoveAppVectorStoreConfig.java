package com.yuluo.yuluoaiagent.rag.config;

/**
 * 配置类：基于本地向量数据库的 RAG 功能
 * 注意：文档加载已迁移到 LoveDocumentInitializer，避免重复插入
 */
// @Configuration
@Deprecated
public class LoveAppVectorStoreConfig {

    // @Resource
    // private LoveAppDocumentLoader loveAppDocumentLoader;
    // @Resource
    // private MyKeywordEnricher myKeywordEnricher;
    // @Resource
    // private VectorStore pgVectorStore;

    // @Bean
    // VectorStore loveAppVectorStore() {
    //     List<Document> documents = loveAppDocumentLoader.loadAllDocuments();
    //     pgVectorStore.add(myKeywordEnricher.enrichDocuments(documents));
    //     return pgVectorStore;
    // }
}
