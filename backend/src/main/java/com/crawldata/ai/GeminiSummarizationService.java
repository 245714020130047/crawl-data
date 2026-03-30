package com.crawldata.ai;

import com.crawldata.model.Article;
import com.crawldata.model.Summary;
import com.crawldata.repository.ArticleRepository;
import com.crawldata.repository.SummarizationConfigRepository;
import com.crawldata.repository.SummaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Primary
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiSummarizationService implements SummarizationService {

    private final ArticleRepository articleRepository;
    private final SummaryRepository summaryRepository;
    private final SummarizationConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.gemini.api-key}")
    private String apiKey;

    @Value("${app.ai.gemini.model:gemini-1.5-flash}")
    private String model;

    @Override
    public void summarize(Article article) {
        if (article.getContent() == null || article.getContent().isBlank()) {
            log.warn("Skipping article {} — no content", article.getId());
            return;
        }

        var config = configRepository.findFirstByOrderByIdAsc().orElse(null);
        int maxTokens = config != null ? config.getMaxTokens() : 500;

        String prompt = buildPrompt(article.getTitle(), article.getContent(), maxTokens);

        try {
            var client = RestClient.create();
            var url = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s"
                    .formatted(model, apiKey);

            var requestBody = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    ))
            );

            var response = client.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsBytes(requestBody))
                    .retrieve()
                    .body(Map.class);

            String text = extractText(response);
            persistSummary(article, text, "GEMINI", model, null);

        } catch (Exception e) {
            log.error("Gemini summarization failed for article {}: {}", article.getId(), e.getMessage());
            article.setStatus("FAILED");
            articleRepository.save(article);
            throw new RuntimeException("Gemini API error", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        var candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "";
        var content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
        var parts = (List<?>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "";
        return (String) ((Map<?, ?>) parts.get(0)).get("text");
    }

    private String buildPrompt(String title, String content, int maxWords) {
        String truncated = content.length() > 5000 ? content.substring(0, 5000) : content;
        return """
                Bạn là trợ lý tóm tắt tin tức tiếng Việt. Hãy tóm tắt bài báo sau trong khoảng %d từ.
                Trả lời theo định dạng:
                TÓM TẮT: <nội dung tóm tắt>
                ĐIỂM CHÍNH:
                - <điểm 1>
                - <điểm 2>
                - <điểm 3>
                
                Tiêu đề: %s
                Nội dung: %s
                """.formatted(maxWords, title, truncated);
    }

    private void persistSummary(Article article, String rawText, String provider, String modelName, Integer tokens) {
        String summaryText = rawText;
        String keyPoints = null;

        int idx = rawText.indexOf("ĐIỂM CHÍNH:");
        if (idx > 0) {
            summaryText = rawText.substring(rawText.indexOf("TÓM TẮT:") + 8, idx).trim();
            keyPoints   = rawText.substring(idx + 11).trim();
        }

        var summary = summaryRepository.findByArticleId(article.getId())
                .orElse(Summary.builder().article(article).build());
        summary.setSummaryText(summaryText);
        summary.setKeyPoints(keyPoints);
        summary.setAiProvider(provider);
        summary.setModelUsed(modelName);
        summary.setTokensUsed(tokens);
        summaryRepository.save(summary);

        article.setStatus("SUMMARIZED");
        articleRepository.save(article);
        log.info("Summarized article {} via {}", article.getId(), provider);
    }
}
