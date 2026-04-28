package com.yuluo.yuluoaiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.yuluo.yuluoaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * PDF 生成工具类
 */
public class PdfGenerateTool {

    private static final String PDF_DIR = FileConstant.FILE_SAVE_DIR + "/pdf";
    private static final String DEFAULT_TITLE = "未命名文档";
    private static final String CJK_FONT_NAME = "STSong-Light";
    private static final String CJK_ENCODING = "UniGB-UCS2-H";

    @Tool(description = "Generate a PDF file from a title and text content")
    public String generatePdf(
            @ToolParam(description = "Name of the PDF file to generate") String fileName,
            @ToolParam(description = "Title of the PDF document") String title,
            @ToolParam(description = "Text content to write into the PDF") String content) {
        validateInput(fileName, content);
        String normalizedFileName = normalizeFileName(fileName);
        String normalizedTitle = StrUtil.blankToDefault(title, DEFAULT_TITLE);
        String filePath = PDF_DIR + "/" + normalizedFileName;

        FileUtil.mkdir(PDF_DIR);
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            Document document = new Document(PageSize.A4, 56, 56, 60, 60);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont baseFont = BaseFont.createFont(CJK_FONT_NAME, CJK_ENCODING, BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font contentFont = new Font(baseFont, 12, Font.NORMAL);

            Paragraph titleParagraph = new Paragraph(normalizedTitle, titleFont);
            titleParagraph.setAlignment(Element.ALIGN_CENTER);
            titleParagraph.setSpacingAfter(18f);
            document.add(titleParagraph);

            Paragraph contentParagraph = new Paragraph();
            contentParagraph.setFont(contentFont);
            contentParagraph.setLeading(20f);
            appendContent(contentParagraph, content, contentFont);
            document.add(contentParagraph);
            document.close();
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF: " + e.getMessage(), e);
        }
        String downloadUrl = "/file/download/pdf/" + normalizedFileName;
        return "PDF generated successfully. Download URL: " + downloadUrl;
    }

    private void appendContent(Paragraph paragraph, String content, Font contentFont) {
        String normalizedContent = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalizedContent.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = StrUtil.emptyToDefault(lines[i], " ");
            paragraph.add(new Chunk(line, contentFont));
            if (i < lines.length - 1) {
                paragraph.add(Chunk.NEWLINE);
            }
        }
    }

    private void validateInput(String fileName, String content) {
        if (StrUtil.isBlank(fileName)) {
            throw new IllegalArgumentException("File name must not be blank");
        }
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("Content must not be blank");
        }
    }

    private String normalizeFileName(String fileName) {
        String trimmedName = fileName.trim();
        return StrUtil.endWithIgnoreCase(trimmedName, ".pdf") ? trimmedName : trimmedName + ".pdf";
    }
}
