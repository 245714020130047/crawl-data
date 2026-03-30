package com.crawldata.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ArticleDto {
    private Long id;
    private String title;
    private String sourceUrl;
    private String sourceName;
    private String imageUrl;
    private String author;
    private LocalDateTime publishedAt;
    private String status;
    private List<String> categories;
    private SummaryDto summary;
}
