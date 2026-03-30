package com.crawldata.controller;

import com.crawldata.dto.ArticleDto;
import com.crawldata.dto.PagedResponse;
import com.crawldata.model.User;
import com.crawldata.repository.UserRepository;
import com.crawldata.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final BookmarkService bookmarkService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .map(User::getId)
                .orElseThrow();
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<PagedResponse<ArticleDto>> getBookmarks(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookmarkService.getBookmarks(getUserId(auth), page, size));
    }

    @PostMapping("/bookmarks/{articleId}")
    public ResponseEntity<Void> addBookmark(@PathVariable Long articleId, Authentication auth) {
        bookmarkService.addBookmark(getUserId(auth), articleId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bookmarks/{articleId}")
    public ResponseEntity<Void> removeBookmark(@PathVariable Long articleId, Authentication auth) {
        bookmarkService.removeBookmark(getUserId(auth), articleId);
        return ResponseEntity.noContent().build();
    }
}
