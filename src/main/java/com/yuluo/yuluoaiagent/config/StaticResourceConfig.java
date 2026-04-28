package com.yuluo.yuluoaiagent.config;

import com.yuluo.yuluoaiagent.constant.FileConstant;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置 - 允许访问生成的文件
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置 tmp 目录为静态资源目录
        String tmpDir = "file:" + FileConstant.FILE_SAVE_DIR + "/";
        registry.addResourceHandler("/static/tmp/**")
                .addResourceLocations(tmpDir)
                .setCachePeriod(0);
    }
}
