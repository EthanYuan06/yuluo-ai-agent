package com.yuluo.yuluoaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.*;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.*;
import reactor.core.publisher.Flux;
import java.util.*;

@Slf4j
public class ForbiddenWordAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    // 违禁词库
    private static final Set<String> FORBIDDEN_WORDS = Set.of("暴力", "色情", "赌博");

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return -100; // 最先执行
    }

    // ====================== 非流式 ======================
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        // 1. 前置检查用户输入
        if (hasForbidden(request.userText())) {
            log.warn("用户输入包含违禁词，已拦截");
            return blockResponse(request);
        }

        // 2. 调用后续Advisor与大模型
        AdvisedResponse response = chain.nextAroundCall(request);

        // 3. 后置检查AI输出
        String aiOutput = response.response().getResult().getOutput().getText();
        if (hasForbidden(aiOutput)) {
            log.warn("AI输出包含违禁词，已拦截");
            return blockResponse(request);
        }

        return response;
    }

    // ====================== 流式 ======================
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        // 前置检查
        if (hasForbidden(request.userText())) {
            log.warn("流式请求违禁词，已拦截");
            return Flux.just(blockResponse(request));
        }

        // 调用后检查每帧输出
        return chain.nextAroundStream(request)
                .map(resp -> {
                    String text = resp.response().getResult().getOutput().getText();
                    if (hasForbidden(text)) {
                        log.warn("流式输出违禁词，已拦截");
                        return blockResponse(request);
                    }
                    return resp;
                });
    }

    // ====================== 工具方法 ======================
    private boolean hasForbidden(String text) {
        return text != null && FORBIDDEN_WORDS.stream().anyMatch(text::contains);
    }

    // 拦截响应
    private AdvisedResponse blockResponse(AdvisedRequest request) {
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("内容违规，已拦截"))
        ));
        return new AdvisedResponse(chatResponse, request.adviseContext());
    }
}