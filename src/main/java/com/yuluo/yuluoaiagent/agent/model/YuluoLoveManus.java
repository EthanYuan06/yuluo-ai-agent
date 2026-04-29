package com.yuluo.yuluoaiagent.agent.model;

import com.yuluo.yuluoaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * Manus实例
 */
@Component
public class YuluoLoveManus extends ToolCallAgent {
    public YuluoLoveManus(ToolCallback[] allTools, ToolCallbackProvider toolCallbackProvider, ChatModel dashscopeChatModel) {
        super(allTools, toolCallbackProvider);
        this.setName("YuluoLoveManus");
        String SYSTEM_PROMPT = """
                You are YuluoLoveManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String nextStepPrompt = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(nextStepPrompt);
        this.setMaxStep(10);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
