package com.crawldata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "summarization_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SummarizationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "provider", length = 20)
    @Builder.Default
    private String provider = "GEMINI";    // GEMINI | OPENAI

    @Column(name = "interval_minutes")
    @Builder.Default
    private Integer intervalMinutes = 15;

    @Column(name = "batch_size")
    @Builder.Default
    private Integer batchSize = 10;

    @Column(name = "max_tokens")
    @Builder.Default
    private Integer maxTokens = 500;
}
