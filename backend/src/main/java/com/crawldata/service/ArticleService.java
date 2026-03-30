package com.crawldata.service;

import com.crawldata.dto.ArticleDto;
import com.crawldata.dto.PagedResponse;
import com.crawldata.dto.SummaryDto;
import com.crawldata.model.Article;
import com.crawldata.repository.ArticleRepository;
import com.crawldata.repository.ReadingHistoryRepository;
import com.crawldata.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ReadingHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "articles", key = "'page:' + #page + ':' + #size")
    public PagedResponse<ArticleDto> getLatestArticles(int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = articleRepository.findByStatusOrderByPublishedAtDesc("SUMMARIZED", pageable);
        var content = result.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PagedResponse<>(content, page, size, result.getTotalElements(),
                result.getTotalPages(), result.isLast());
    }

    @Cacheable(value = "articles", key = "#id")
    public ArticleDto getById(Long id) {
        var article = articleRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Article not found: " + id));
        return toDto(article);
    }

    @Cacheable(value = "articles", key = "'category:' + #categoryId + ':' + #page")
    public PagedResponse<ArticleDto> getByCategory(Long categoryId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = articleRepository.findByCategoryId(categoryId, pageable);
        var content = result.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PagedResponse<>(content, page, size, result.getTotalElements(),
                result.getTotalPages(), result.isLast());
    }

    @Transactional
    public void recordRead(Long articleId, String username) {
        var article = articleRepository.findById(articleId).orElse(null);
        if (article == null) return;
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;
        var history = com.crawldata.model.ReadingHistory.builder()
                .user(user).article(article).build();
        historyRepository.save(history);
    }

    public ArticleDto toDto(Article a) {
        var builder = ArticleDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .sourceUrl(a.getSourceUrl())
                .sourceName(a.getSource() != null ? a.getSource().getName() : null)
                .imageUrl(a.getImageUrl())
                .author(a.getAuthor())
                .publishedAt(a.getPublishedAt())
                .status(a.getStatus())
                .categories(a.getCategories().stream()
                        .map(c -> c.getName()).collect(Collectors.toList()));

        if (a.getSummary() != null) {
            var s = a.getSummary();
            builder.summary(SummaryDto.builder()
                    .summaryText(s.getSummaryText())
                    .keyPoints(s.getKeyPoints())
                    .aiProvider(s.getAiProvider())
                    .build());
        }
        return builder.build();
    }
}
