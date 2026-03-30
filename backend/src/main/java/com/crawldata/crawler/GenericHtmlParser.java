package com.crawldata.crawler;

import com.crawldata.model.Article;
import com.crawldata.model.CrawlerSource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Component
@Slf4j
public class GenericHtmlParser {

    public Article parse(CrawlerSource source, String url, Element rssItem) {
        var selectors = source.getSelectorConfig();
        String fingerprint = sha256(url);

        try {
            var doc = Jsoup.connect(url)
                    .userAgent("CrawlDataBot/1.0")
                    .timeout(20_000)
                    .get();

            String title   = rssItem.select("title").text();
            if (title.isBlank()) title = doc.title();

            String content = selectors != null && selectors.containsKey("content")
                    ? doc.select(selectors.get("content")).text()
                    : doc.select("article, .article-content, .content, #content").text();

            String imageUrl = selectors != null && selectors.containsKey("image")
                    ? doc.select(selectors.get("image")).attr("src")
                    : doc.select("meta[property=og:image]").attr("content");

            String author = selectors != null && selectors.containsKey("author")
                    ? doc.select(selectors.get("author")).text()
                    : doc.select(".author, .byline, [itemprop=author]").text();

            LocalDateTime publishedAt = parseDate(rssItem.select("pubDate").text());

            return Article.builder()
                    .title(title)
                    .sourceUrl(url)
                    .source(source)
                    .content(content.isBlank() ? null : content)
                    .imageUrl(imageUrl.isBlank() ? null : imageUrl)
                    .author(author.isBlank() ? null : author)
                    .publishedAt(publishedAt)
                    .urlFingerprint(fingerprint)
                    .status("RAW")
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse article at {}: {}", url, e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDateTime.now();
        try {
            // RFC 2822  e.g. "Thu, 01 Jan 2024 12:00:00 +0700"
            var formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
