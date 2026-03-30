package com.crawldata.repository;

import com.crawldata.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySourceUrl(String sourceUrl);
    Optional<Article> findByUrlFingerprint(String fingerprint);

    Page<Article> findByStatusOrderByPublishedAtDesc(String status, Pageable pageable);

    @Query("SELECT a FROM Article a JOIN a.categories c WHERE c.id = :categoryId ORDER BY a.publishedAt DESC")
    Page<Article> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = """
            SELECT * FROM articles
            WHERE to_tsvector('simple', title || ' ' || coalesce(content, ''))
                  @@ plainto_tsquery('simple', :query)
            ORDER BY published_at DESC
            """, nativeQuery = true)
    Page<Article> fullTextSearch(@Param("query") String query, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.status = 'RAW' ORDER BY a.createdAt ASC")
    List<Article> findUnsummarized(Pageable pageable);

    long countByStatus(String status);
}
