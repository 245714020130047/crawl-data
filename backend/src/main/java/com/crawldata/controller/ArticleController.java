package com.crawldata.controller;

import com.crawldata.dto.ArticleDto;
import com.crawldata.dto.PagedResponse;
import com.crawldata.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<PagedResponse<ArticleDto>> getLatest(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(articleService.getLatestArticles(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDto> getById(@PathVariable Long id, Authentication auth) {
        var dto = articleService.getById(id);
        if (auth != null) {
            articleService.recordRead(id, auth.getName());
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<PagedResponse<ArticleDto>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(articleService.getByCategory(categoryId, page, size));
    }
}
