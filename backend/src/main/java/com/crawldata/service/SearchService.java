package com.crawldata.service;

import com.crawldata.dto.ArticleDto;
import com.crawldata.dto.PagedResponse;
import com.crawldata.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ArticleRepository articleRepository;
    private final ArticleService articleService;

    @Cacheable(value = "search", key = "#query + ':' + #page")
    public PagedResponse<ArticleDto> search(String query, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = articleRepository.fullTextSearch(query, pageable);
        var content = result.getContent().stream()
                .map(articleService::toDto)
                .collect(Collectors.toList());
        return new PagedResponse<>(content, page, size, result.getTotalElements(),
                result.getTotalPages(), result.isLast());
    }
}
