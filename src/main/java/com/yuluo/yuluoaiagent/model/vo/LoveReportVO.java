package com.yuluo.yuluoaiagent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoveReportVO {
    /**
     * 报告标题
     */
    private String title;

    /**
     * 建议列表
     */
    private List<String> suggestions;
}
