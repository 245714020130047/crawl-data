package com.crawldata.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrawlJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private CrawlerSource source;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING";  // PENDING | RUNNING | DONE | FAILED

    @Column(name = "articles_found")
    @Builder.Default
    private Integer articlesFound = 0;

    @Column(name = "articles_saved")
    @Builder.Default
    private Integer articlesSaved = 0;

    @Column(name = "duplicate_count")
    @Builder.Default
    private Integer duplicateCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
