package com.yuluo.yuluoaiagent.controller;

import com.yuluo.yuluoaiagent.constant.FileConstant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件下载控制器
 */
@RestController
@RequestMapping("/file")
public class FileDownloadController {

    private static final String FILE_DIR = FileConstant.FILE_SAVE_DIR;

    /**
     * 下载文件（支持 Markdown 和 PDF）
     *
     * @param fileType 文件类型（file 或 pdf）
     * @param fileName 文件名
     * @param response HTTP 响应
     * @return 文件资源
     */
    @GetMapping("/download/{fileType}/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileType,
            @PathVariable String fileName,
            HttpServletResponse response) {
        
        try {
            // 构建文件路径
            String filePath = FILE_DIR + "/" + fileType + "/" + fileName;
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            // 检查文件是否存在且可读
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // 确定 Content-Type
            MediaType mediaType;
            if (fileName.toLowerCase().endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if (fileName.toLowerCase().endsWith(".md")) {
                mediaType = MediaType.parseMediaType("text/markdown");
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            // 编码文件名（处理中文等特殊字符）
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取文件访问 URL
     *
     * @param fileType 文件类型（file 或 pdf）
     * @param fileName 文件名
     * @return 完整的下载 URL
     */
    @GetMapping("/url/{fileType}/{fileName}")
    public ResponseEntity<String> getFileUrl(
            @PathVariable String fileType,
            @PathVariable String fileName) {
        
        String url = "/file/download/" + fileType + "/" + fileName;
        return ResponseEntity.ok(url);
    }
}
