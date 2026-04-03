package com.yuluo.yuluoaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
class LoveAppDocumentLoader {

    // 支持通配符的资源加载接口
    private final ResourcePatternResolver resourcePatternResolver;

    LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 仅加载候选人相关文档（用于推荐功能）
     */
    public List<Document> loadCandidateDocuments() {
        return loadDocumentsByPattern("classpath:document/恋爱候选人*.md");
    }

    /**
     * 仅加载常见问题与回答文档（用于问答功能）
     */
    public List<Document> loadQADocuments() {
        return loadDocumentsByPattern("classpath:document/恋爱常见问题与回答*.md");
    }

    private List<Document> loadDocumentsByPattern(String pattern) {
        List<Document> documents = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                assert fileName != null;
                String gender = extractGenderFromFileName(fileName);
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", fileName)
                        .withAdditionalMetadata("gender", gender)
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                documents.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("加载文档失败 [pattern={}]", pattern, e);
        }
        return documents;
    }

    /**
     * 从文件名中提取性别信息
     *
     * @param fileName 文件名
     * @return         性别
     */
    private String extractGenderFromFileName(String fileName) {
        if (fileName.contains("女生")) {
            return "female";
        } else if (fileName.contains("男生")) {
            return "male";
        }
        return "unknown";
    }
}