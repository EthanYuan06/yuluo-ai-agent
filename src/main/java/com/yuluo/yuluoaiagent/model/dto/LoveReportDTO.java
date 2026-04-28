package com.yuluo.yuluoaiagent.model.dto;

import java.util.List;

public record LoveReportDTO(
        String title,
        List<String> suggestions
) {
}