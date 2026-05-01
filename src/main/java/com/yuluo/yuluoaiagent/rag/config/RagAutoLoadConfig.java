package com.yuluo.yuluoaiagent.rag.config;

import com.yuluo.yuluoaiagent.rag.LoveDocumentInitializer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RagAutoLoadConfig {

    @Resource
    private LoveDocumentInitializer documentInitializer;

    /**
     * 数据库状态监听器
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("🔍 检查向量数据库状态...");
        documentInitializer.initializeIfEmpty();
    }
}
