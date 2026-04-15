package com.yuluo.yuluoaiagent.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatModelConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Bean
    DashScopeApi dashScopeApi() {
        return new DashScopeApi(apiKey);
    }
    @Bean
    @Primary
    ChatModel textChatModel(DashScopeApi dashScopeApi) {
        return new DashScopeChatModel(
                dashScopeApi,
                DashScopeChatOptions.builder()
                        .withModel("qwen-max")
                        .withIncrementalOutput(true)
                        .build()
        );
    }

    @Bean
    ChatModel visionChatModel(DashScopeApi dashScopeApi) {
        return new DashScopeChatModel(
                dashScopeApi,
                DashScopeChatOptions.builder()
                        .withModel("qwen3-vl-plus")
                        .withMultiModel(true)
                        .withIncrementalOutput(true)
                        .build()
        );
    }
}
