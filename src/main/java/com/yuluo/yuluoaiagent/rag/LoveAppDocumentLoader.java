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

    private final ResourcePatternResolver resourcePatternResolver;

    LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载所有恋爱相关文档（统一加载到向量数据库）
     */
    public List<Document> loadAllDocuments() {
        return loadDocumentsByPattern("classpath:document/*.md");
    }

    private List<Document> loadDocumentsByPattern(String pattern) {
        List<Document> documents = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            log.info("找到 {} 个Markdown文件", resources.length);

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                assert fileName != null;

                String docType = extractDocTypeFromFileName(fileName);
                String status = extractStatusFromFileName(fileName);
                String gender = extractGenderFromFileName(fileName);

                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("docType", docType)
                        .withAdditionalMetadata("gender", gender)
                        .withAdditionalMetadata("status", status)
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                List<Document> docs = reader.get();
                documents.addAll(docs);
                
                log.debug("加载文件: {}, 生成 {} 个文档切片", fileName, docs.size());
            }
            
            log.info("总共加载 {} 个文档切片", documents.size());

        } catch (IOException e) {
            log.error("加载文档失败 [pattern={}]", pattern, e);
        }
        return documents;
    }

    /**
     * 从文件名提取文档类型
     * 例如: 恋爱候选人女生_01.md -> "candidate"
     *      恋爱常见问题与回答_01.md -> "qa"
     *      其他恋爱文档.md -> "general"
     */
    private String extractDocTypeFromFileName(String fileName) {
        if (fileName.contains("候选人")) {
            return "candidate";
        } else if (fileName.contains("常见问题") || fileName.contains("问答")) {
            return "qa";
        }
        return "general";
    }

    /**
     * 从文件名中提取状态（横杠后的内容）
     * 例如: 恋爱常见问题与回答-单身篇.md -> "单身"
     */
    private String extractStatusFromFileName(String fileName) {
        if (fileName.contains("-")) {
            int dashIndex = fileName.lastIndexOf("-");
            int dotIndex = fileName.lastIndexOf(".");
            if (dashIndex < dotIndex - 1) {
                return fileName.substring(dashIndex + 1, dotIndex - 1);
            }
        }
        return "default";
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
