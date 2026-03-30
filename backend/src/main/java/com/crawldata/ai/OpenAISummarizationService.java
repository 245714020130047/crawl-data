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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAISummarizationService implements SummarizationService {

    private final ArticleRepository articleRepository;
    private final SummaryRepository summaryRepository;
    private final SummarizationConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.openai.api-key}")
    private String apiKey;

    @Value("${app.ai.openai.model:gpt-3.5-turbo}")
    private String model;

    @Override
    public void summarize(Article article) {
        if (article.getContent() == null || article.getContent().isBlank()) {
            log.warn("Skipping article {} — no content", article.getId());
            return;
        }

        var config = configRepository.findFirstByOrderByIdAsc().orElse(null);
        int maxTokens = config != null ? config.getMaxTokens() : 500;

        String truncated = article.getContent().length() > 5000
                ? article.getContent().substring(0, 5000)
                : article.getContent();

        String userMsg = """
                Tóm tắt bài báo sau trong %d từ. Định dạng:
                TÓM TẮT: <nội dung>
                ĐIỂM CHÍNH:
                - <điểm 1>
                - <điểm 2>
                
                Tiêu đề: %s
                Nội dung: %s
                """.formatted(maxTokens, article.getTitle(), truncated);

        try {
            var client = RestClient.create();
            var requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "Bạn là trợ lý tóm tắt tin tức tiếng Việt."),
                            Map.of("role", "user",   "content", userMsg)
                    ),
                    "max_tokens", maxTokens
            );

            var response = client.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsBytes(requestBody))
                    .retrieve()
                    .body(Map.class);

            String text = extractText(response);
            int tokensUsed = extractTokens(response);
            persistSummary(article, text, "OPENAI", model, tokensUsed);

        } catch (Exception e) {
            log.error("OpenAI summarization failed for article {}: {}", article.getId(), e.getMessage());
            article.setStatus("FAILED");
            articleRepository.save(article);
            throw new RuntimeException("OpenAI API error", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        var choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "";
        var message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        return (String) message.get("content");
    }

    @SuppressWarnings("unchecked")
    private int extractTokens(Map<?, ?> response) {
        try {
            var usage = (Map<?, ?>) response.get("usage");
            return ((Number) usage.get("total_tokens")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private void persistSummary(Article article, String rawText, String provider, String modelName, Integer tokens) {
        String summaryText = rawText;
        String keyPoints   = null;
        int idx = rawText.indexOf("ĐIỂM CHÍNH:");
        if (idx > 0) {
            int start = rawText.indexOf("TÓM TẮT:");
            summaryText = (start >= 0) ? rawText.substring(start + 8, idx).trim() : rawText.substring(0, idx).trim();
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
        log.info("Summarized article {} via OpenAI", article.getId());
    }
}
