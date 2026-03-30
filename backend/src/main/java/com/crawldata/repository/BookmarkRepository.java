package com.crawldata.repository;

import com.crawldata.model.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Page<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<Bookmark> findByUserIdAndArticleId(Long userId, Long articleId);
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);
    void deleteByUserIdAndArticleId(Long userId, Long articleId);
}
