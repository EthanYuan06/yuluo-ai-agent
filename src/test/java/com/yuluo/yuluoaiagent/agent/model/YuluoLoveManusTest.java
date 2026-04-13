package com.yuluo.yuluoaiagent.agent.model;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Vector;

@SpringBootTest
class YuManusTest {

    @Resource
    private YuluoLoveManus yuluoLoveManus;

    @MockitoBean
    private VectorStore loveAppVectorStore;
    @MockitoBean
    private VectorStore candidateVectorStore;

    @Test
    void run() {
        String userPrompt = """  
                我想在上海的高档餐厅约会，要求能观看黄浦江的夜景，
                请帮我找到黄浦区适合的地点，
                并生成markdown文件，命名“高档餐厅约会”
                """;
        String answer = yuluoLoveManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}
