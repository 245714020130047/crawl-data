package com.crawldata.service;

import com.crawldata.dto.ArticleDto;
import com.crawldata.dto.PagedResponse;
import com.crawldata.model.Bookmark;
import com.crawldata.repository.ArticleRepository;
import com.crawldata.repository.BookmarkRepository;
import com.crawldata.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ArticleService articleService;

    @Transactional
    public void addBookmark(Long userId, Long articleId) {
        if (bookmarkRepository.existsByUserIdAndArticleId(userId, articleId)) return;
        var user    = userRepository.findById(userId).orElseThrow();
        var article = articleRepository.findById(articleId).orElseThrow();
        bookmarkRepository.save(Bookmark.builder().user(user).article(article).build());
    }

    @Transactional
    public void removeBookmark(Long userId, Long articleId) {
        bookmarkRepository.deleteByUserIdAndArticleId(userId, articleId);
    }

    public PagedResponse<ArticleDto> getBookmarks(Long userId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result   = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        var content  = result.getContent().stream()
                .map(b -> articleService.toDto(b.getArticle()))
                .collect(Collectors.toList());
        return new PagedResponse<>(content, page, size, result.getTotalElements(),
                result.getTotalPages(), result.isLast());
    }
}
