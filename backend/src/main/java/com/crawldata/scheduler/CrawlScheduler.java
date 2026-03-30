package com.crawldata.scheduler;

import com.crawldata.messaging.CrawlJobPublisher;
import com.crawldata.messaging.SummarizeJobPublisher;
import com.crawldata.repository.ArticleRepository;
import com.crawldata.repository.CrawlerSourceRepository;
import com.crawldata.repository.SummarizationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CrawlScheduler {

    private final CrawlerSourceRepository sourceRepository;
    private final ArticleRepository articleRepository;
    private final SummarizationConfigRepository configRepository;
    private final CrawlJobPublisher crawlJobPublisher;
    private final SummarizeJobPublisher summarizeJobPublisher;

    /** Publish crawl jobs for all active sources every hour */
    @Scheduled(fixedRateString = "${app.crawler.schedule-interval-ms:3600000}")
    public void scheduleCrawls() {
        var sources = sourceRepository.findByStatus("ACTIVE");
        log.info("Scheduling crawl for {} active sources", sources.size());
        sources.forEach(s -> crawlJobPublisher.publish(s.getId()));
    }

    /** Dispatch unsummarized articles every N minutes, based on DB config */
    @Scheduled(fixedDelayString = "60000")
    public void scheduleSummarization() {
        var config = configRepository.findFirstByOrderByIdAsc().orElse(null);
        if (config == null || !config.getIsEnabled()) return;

        var unsummarized = articleRepository.findUnsummarized(
                PageRequest.of(0, config.getBatchSize()));
        if (!unsummarized.isEmpty()) {
            log.info("Queuing {} articles for summarization", unsummarized.size());
            unsummarized.forEach(a -> summarizeJobPublisher.publish(a.getId()));
        }
    }
}
