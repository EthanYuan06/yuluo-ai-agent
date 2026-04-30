package com.yuluo.yuluoaiagent;


import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
public class YuluoAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuluoAiAgentApplication.class, args);
    }
}
