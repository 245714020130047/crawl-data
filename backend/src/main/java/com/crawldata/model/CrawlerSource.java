package com.crawldata.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "crawler_sources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrawlerSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String baseUrl;

    @Column(nullable = false)
    private String rssUrl;

    @Column(length = 50)
    @Builder.Default
    private String status = "ACTIVE";   // ACTIVE | PAUSED | DISABLED

    @Column(name = "crawl_interval_minutes")
    @Builder.Default
    private Integer crawlIntervalMinutes = 60;

    @Column(name = "request_delay_ms")
    @Builder.Default
    private Integer requestDelayMs = 1000;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selector_config", columnDefinition = "jsonb")
    private Map<String, String> selectorConfig;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;
}
