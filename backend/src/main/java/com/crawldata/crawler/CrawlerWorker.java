package com.crawldata.crawler;

import com.crawldata.messaging.SummarizeJobPublisher;
import com.crawldata.model.Article;
import com.crawldata.model.CrawlJob;
import com.crawldata.model.CrawlerSource;
import com.crawldata.repository.ArticleRepository;
import com.crawldata.repository.CrawlJobRepository;
import com.crawldata.repository.CrawlerSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class CrawlerWorker {

    private final CrawlerSourceRepository sourceRepository;
    private final ArticleRepository articleRepository;
    private final CrawlJobRepository crawlJobRepository;
    private final DuplicateDetectionService deduplication;
    private final GenericHtmlParser htmlParser;
    private final SummarizeJobPublisher summarizeJobPublisher;

    public void execute(Long sourceId) {
        var source = sourceRepository.findById(sourceId).orElseThrow();
        var job = CrawlJob.builder().source(source).status("RUNNING").build();
        job = crawlJobRepository.save(job);

        int saved = 0, duplicates = 0;

        try {
            var rssDoc = Jsoup.connect(source.getRssUrl())
                    .userAgent("CrawlDataBot/1.0")
                    .timeout(30_000)
                    .get();

            var items = rssDoc.select("item");
            job.setArticlesFound(items.size());

            for (var item : items) {
                String link = item.select("link").text();
                if (link.isBlank()) link = item.select("guid").text();

                // Fast duplicate check via Redis
                if (deduplication.isDuplicate(link)) {
                    duplicates++;
                    continue;
                }

                // Parse full article HTML
                var article = htmlParser.parse(source, link, item);
                if (article == null) { duplicates++; continue; }

                try {
                    articleRepository.save(article);
                    deduplication.markSeen(link);
                    summarizeJobPublisher.publish(article.getId());
                    saved++;
                } catch (DataIntegrityViolationException e) {
                    // URL already exists in DB — fallback dedup
                    duplicates++;
                }

                Thread.sleep(source.getRequestDelayMs());
            }

            source.setLastCrawledAt(LocalDateTime.now());
            sourceRepository.save(source);

            job.setStatus("DONE");
            job.setArticlesSaved(saved);
            job.setDuplicateCount(duplicates);
            job.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Crawl failed for source {}: {}", source.getName(), e.getMessage());
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            throw new RuntimeException("Crawl failed", e);
        } finally {
            crawlJobRepository.save(job);
        }
    }
}
