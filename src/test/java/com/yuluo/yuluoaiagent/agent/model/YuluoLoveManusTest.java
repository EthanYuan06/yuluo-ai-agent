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

    // @Test
    // void run() {
    //     String userPrompt = """
    //             我想在上海和女朋友一起找酒店
    //             请帮我找到适合情侣居住的酒店，要求三星级以上，每晚预算不超过500元，
    //             并附带酒店的外景图，
    //             生成markdown文件，命名“适合情侣在上海居住的酒店”
    //             """;
    //     String answer = yuluoLoveManus.run(userPrompt);
    //     Assertions.assertNotNull(answer);
    // }
}
