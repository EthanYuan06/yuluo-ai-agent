package com.yuluo.yuluoaiagent.agent.model;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.agent.Agent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程
 * 功能：状态转换、内存管理、基于步骤的执行流程
 * 子类必须实现 step()方法
 */
@Data
@Slf4j
public abstract class BaseAgent {
    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 状态
    private AgentState state = AgentState.IDLE;

    // 执行控制
    private int maxStep = 10;
    private int currentStep = 0;

    // 对话客户端
    private ChatClient chatClient;

    // 对话记忆
    private List<Message> messageList = new ArrayList<>();

    public String run(String userPrompt) {
        // 基础检查
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("User prompt cannot be empty.");
        }
        // 更改状态
        state = AgentState.RUNNING;
        // 记录消息上下文，将用户消息添加到消息列表中
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxStep && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/ " + maxStep);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
                // 检查是否超出步骤限制
                if (stepNumber >= maxStep) {
                    state = AgentState.FINISHED;
                    results.add("Maximum number of steps reached.");
                }
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent: ", e);
            return "执行错误: " + e.getMessage();
        } finally {
            // 清理资源
            this.cleanup();
        }
    }

    /**
     * 执行单个步骤
     *
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup(){};
}
