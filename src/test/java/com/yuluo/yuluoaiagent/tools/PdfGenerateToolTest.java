package com.yuluo.yuluoaiagent.tools;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfGenerateToolTest {

    @Test
    void testGeneratePdf() {
        PdfGenerateTool tool = new PdfGenerateTool();
        String fileName = "pdf-test-" + UUID.randomUUID();
        String result = tool.generatePdf(
                fileName,
                "测试标题",
                "第一行内容\n第二行内容\n这里是一段用于生成 PDF 的中文文本。"
        );

        File generatedFile = new File(System.getProperty("user.dir") + "/tmp/pdf/" + fileName + ".pdf");
        String expectedPath = generatedFile.getAbsolutePath();

        assertTrue(result.contains(expectedPath));
        assertTrue(FileUtil.exist(generatedFile));
        assertTrue(generatedFile.length() > 0);
    }
}
