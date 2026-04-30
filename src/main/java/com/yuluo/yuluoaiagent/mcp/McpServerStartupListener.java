package com.yuluo.yuluoaiagent.mcp;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * MCP Server 启动监听器
 * 在应用完全启动后触发MCP Server子进程启动
 */
@Component
public class McpServerStartupListener {

    private final McpServerStarter mcpServerStarter;

    public McpServerStartupListener(McpServerStarter mcpServerStarter) {
        this.mcpServerStarter = mcpServerStarter;
    }

    /**
     * 监听应用就绪事件，启动MCP Server子进程
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        mcpServerStarter.start();
    }
}
