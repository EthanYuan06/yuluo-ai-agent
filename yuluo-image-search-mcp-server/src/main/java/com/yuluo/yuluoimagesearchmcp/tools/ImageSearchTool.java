package com.yuluo.yuluoimagesearchmcp.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pexels 图片搜索 MCP
 */
@Service
public class ImageSearchTool {

    private static final String API_KEY = System.getenv("api_key");
    private static final String API_URL = "https://api.pexels.com/v1/search";

    @Tool(description = "search image from web")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            // 直接调用中等尺寸图片列表方法
            return String.join(",", searchMediumImages(query));
        } catch (Exception e) {
            return "search image error: " + e.getMessage();
        }
    }

    /**
     * 新增方法：搜索中等尺寸图片列表（返回List<String>）
     */
    public List<String> searchMediumImages(String query) {
        // 请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", System.getenv(API_KEY));

        // 请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);

        // 发起请求
        String resp = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        // 解析提取medium尺寸图片链接
        return JSONUtil.parseObj(resp)
                .getJSONArray("photos")
                .stream()
                .map(obj -> JSONUtil.parseObj(obj).getJSONObject("src"))
                .map(src -> src.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}