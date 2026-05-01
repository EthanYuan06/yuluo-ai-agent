package com.yuluo.yuluoaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LoveDocumentInitializer {

    @Resource
    private LoveAppDocumentLoader documentLoader;
    
    @Resource
    private MyKeywordEnricher keywordEnricher;
    
    @Resource
    private VectorStore pgVectorStore;
    
    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 智能加载文档：只在数据库为空时加载
     */
    public void initializeIfEmpty() {
        try {
            // 检查是否已有数据
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM love_document_store",
                Integer.class
            );
            
            if (count != null && count > 0) {
                log.info("✅ 向量数据库已有 {} 条记录，无需加载", count);
                return;
            }
            
            // 首次加载
            log.info("📚 数据库为空，开始加载文档...");
            loadDocuments();
            
        } catch (Exception e) {
            log.error("❌ 检查或加载文档失败", e);
        }
    }

    /**
     * 执行文档加载
     */
    private void loadDocuments() {
        long startTime = System.currentTimeMillis();
        
        // 1. 加载所有文档
        List<Document> allDocuments = documentLoader.loadAllDocuments();
        
        if (allDocuments.isEmpty()) {
            log.warn("⚠️ 未找到任何文档");
            return;
        }
        
        log.info("找到 {} 个原始文档，正在进行关键词增强...", allDocuments.size());
        
        // 2. 关键词增强
        List<Document> enrichedDocs = keywordEnricher.enrichDocuments(allDocuments);
        
        // 3. 写入向量数据库
        log.info("正在写入向量数据库 [love_document_store]...");
        pgVectorStore.add(enrichedDocs);
        
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ 成功加载 {} 个文档切片，耗时: {}ms", enrichedDocs.size(), elapsed);
    }
}
