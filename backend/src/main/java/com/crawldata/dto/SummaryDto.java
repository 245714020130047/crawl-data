package com.crawldata.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class SummaryDto {
    private String summaryText;
    private String keyPoints;
    private String aiProvider;
}
