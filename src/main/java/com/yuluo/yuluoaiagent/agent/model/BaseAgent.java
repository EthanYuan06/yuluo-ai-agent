package com.yuluo.yuluoaiagent.agent.model;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.agent.Agent;
import com.yuluo.yuluoaiagent.exception.BusinessException;
import com.yuluo.yuluoaiagent.exception.ErrorCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程
 * 功能：状态转换、内存管理、基于步骤的执行流程
 * 子类必须实现 step()方法
 */
@Data
@Slf4j
public abstract class BaseAgent {
    // 代理名称
    private String name;

    // 系统提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 状态
    private AgentState state = AgentState.IDLE;

    // 执行控制
    private int maxStep = 10;
    private int currentStep = 0;

    // 大模型客户端
    private ChatClient chatClient;

    // 对话上下文
    private List<Message> messageList = new ArrayList<>();

    public SseEmitter runStream(String userPrompt) {
        // 创建5分钟超时的emitter
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 基础检查
                if (this.state != AgentState.IDLE) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "代理状态异常，无法执行");
                }
                if (StrUtil.isBlank(userPrompt)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户提示词不能为空");
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
                        log.info("Executing step " + stepNumber + " / " + maxStep);
                        // 单步执行
                        String stepResult = step();
                        String result = "Step " + stepNumber + " : " + stepResult;
                        emitter.send(result);


                    }
                    // 检查是否超出步骤限制
                    if (currentStep >= maxStep) {
                        state = AgentState.FINISHED;
                        emitter.send("达到最大思考次数上限");
                    }
                    // 正常完成，记得释放连接
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("智能体执行失败： ", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (IOException ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.cleanup();
                }
            }catch (Exception e){
                emitter.completeWithError(e);
            }
        });

       //  设置超时与完成回调
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out.");
        });
        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed.");
        });
       return emitter;
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