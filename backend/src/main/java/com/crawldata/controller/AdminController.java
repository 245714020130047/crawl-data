package com.crawldata.controller;

import com.crawldata.messaging.CrawlJobPublisher;
import com.crawldata.messaging.SummarizeJobPublisher;
import com.crawldata.model.CrawlerSource;
import com.crawldata.model.SummarizationConfig;
import com.crawldata.repository.ArticleRepository;
import com.crawldata.repository.CrawlJobRepository;
import com.crawldata.repository.CrawlerSourceRepository;
import com.crawldata.repository.SummarizationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CrawlerSourceRepository sourceRepository;
    private final CrawlJobRepository crawlJobRepository;
    private final ArticleRepository articleRepository;
    private final SummarizationConfigRepository configRepository;
    private final CrawlJobPublisher crawlJobPublisher;
    private final SummarizeJobPublisher summarizeJobPublisher;

    // ─── Dashboard stats ──────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "totalArticles",      articleRepository.count(),
                "summarizedArticles", articleRepository.countByStatus("SUMMARIZED"),
                "rawArticles",        articleRepository.countByStatus("RAW"),
                "activeSources",      sourceRepository.findByStatus("ACTIVE").size(),
                "recentJobs",         crawlJobRepository.findByOrderByStartedAtDesc(PageRequest.of(0, 10)).getContent()
        ));
    }

    // ─── Crawler sources ──────────────────────────────────────────────────────
    @GetMapping("/sources")
    public ResponseEntity<List<CrawlerSource>> getSources() {
        return ResponseEntity.ok(sourceRepository.findAll());
    }

    @PostMapping("/sources")
    public ResponseEntity<CrawlerSource> createSource(@RequestBody CrawlerSource source) {
        return ResponseEntity.ok(sourceRepository.save(source));
    }

    @PutMapping("/sources/{id}")
    public ResponseEntity<CrawlerSource> updateSource(@PathVariable Long id,
                                                       @RequestBody CrawlerSource body) {
        var source = sourceRepository.findById(id).orElseThrow();
        source.setName(body.getName());
        source.setBaseUrl(body.getBaseUrl());
        source.setRssUrl(body.getRssUrl());
        source.setStatus(body.getStatus());
        source.setCrawlIntervalMinutes(body.getCrawlIntervalMinutes());
        source.setRequestDelayMs(body.getRequestDelayMs());
        source.setSelectorConfig(body.getSelectorConfig());
        return ResponseEntity.ok(sourceRepository.save(source));
    }

    @DeleteMapping("/sources/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable Long id) {
        sourceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Manual trigger ───────────────────────────────────────────────────────
    @PostMapping("/crawl/trigger")
    public ResponseEntity<Map<String, String>> triggerCrawl(
            @RequestParam(required = false) Long sourceId) {
        List<CrawlerSource> sources = sourceId != null
                ? sourceRepository.findById(sourceId).map(List::of).orElse(List.of())
                : sourceRepository.findByStatus("ACTIVE");
        sources.forEach(s -> crawlJobPublisher.publish(s.getId()));
        return ResponseEntity.ok(Map.of("message", "Crawl jobs enqueued for " + sources.size() + " sources"));
    }

    @PostMapping("/summarization/trigger")
    public ResponseEntity<Map<String, String>> triggerSummarization() {
        var articles = articleRepository.findUnsummarized(PageRequest.of(0, 50));
        articles.forEach(a -> summarizeJobPublisher.publish(a.getId()));
        return ResponseEntity.ok(Map.of("message", "Summarize jobs enqueued: " + articles.size()));
    }

    // ─── Summarization config ─────────────────────────────────────────────────
    @GetMapping("/summarization/config")
    public ResponseEntity<SummarizationConfig> getConfig() {
        return ResponseEntity.ok(configRepository.findFirstByOrderByIdAsc().orElseThrow());
    }

    @PutMapping("/summarization/config")
    public ResponseEntity<SummarizationConfig> updateConfig(@RequestBody SummarizationConfig config) {
        var existing = configRepository.findFirstByOrderByIdAsc().orElse(config);
        existing.setIsEnabled(config.getIsEnabled());
        existing.setProvider(config.getProvider());
        existing.setIntervalMinutes(config.getIntervalMinutes());
        existing.setBatchSize(config.getBatchSize());
        existing.setMaxTokens(config.getMaxTokens());
        return ResponseEntity.ok(configRepository.save(existing));
    }
}
