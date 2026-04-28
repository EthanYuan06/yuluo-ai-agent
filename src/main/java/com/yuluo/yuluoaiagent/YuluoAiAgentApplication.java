package com.yuluo.yuluoaiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class YuluoAiAgentApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(YuluoAiAgentApplication.class, args);
        
        // 在应用启动完成后启动MCP Server子进程
        Environment env = applicationContext.getBean(Environment.class);
        startMcpServerSubprocess(env);
    }

    /**
     * 启动MCP Server子进程
     */
    private static void startMcpServerSubprocess(Environment env) {
        try {
            // 获取当前应用的工作目录
            String workingDir = System.getProperty("user.dir", ".");
            
            // 获取JAR文件路径
            String jarPath = workingDir + "/yuluo-image-search-mcp-server/target/yuluo-image-search-mcp-server-0.0.1-SNAPSHOT.jar";
            File jarFile = new File(jarPath);

            // 构建MCP Server子进程启动命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                "java",
                "-Dspring.ai.mcp.server.stdio=true",
                "-Dspring.main.web-application-type=none",
                "-Dlogging.pattern.console=",
                "-jar",
                jarPath
            );
            
            // 设置工作目录
            processBuilder.directory(new File(workingDir));

            // 从配置文件中获取Pexels API密钥并传递给子进程
            String apiKey = env.getProperty("pexels.api-key");
            processBuilder.environment().put("api_key", apiKey);

            // 启动MCP Server子进程
            Process mcpServerProcess = processBuilder.start();
            
            // 异步处理子进程的输出和错误流
            CompletableFuture.runAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(mcpServerProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[MCP Server] " + line);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading MCP Server output: " + e.getMessage());
                }
            });
            
            CompletableFuture.runAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(mcpServerProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("[MCP Server ERROR] " + line);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading MCP Server error stream: " + e.getMessage());
                }
            });
            
            // 监听主应用关闭事件，优雅地关闭子进程
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down MCP Server sub-process...");
                if (mcpServerProcess.isAlive()) {
                    mcpServerProcess.destroy();
                    try {
                        mcpServerProcess.waitFor();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Interrupted while waiting for MCP Server to shut down: " + e.getMessage());
                    }
                }
                System.out.println("MCP Server sub-process shutdown complete.");
            }));
            
            System.out.println("MCP Server started as sub-process successfully!");
            
        } catch (IOException e) {
            System.err.println("Failed to start MCP Server sub-process: " + e.getMessage());
        }
    }
}
