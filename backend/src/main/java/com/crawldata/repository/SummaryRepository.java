package com.crawldata.repository;

import com.crawldata.model.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    Optional<Summary> findByArticleId(Long articleId);
    boolean existsByArticleId(Long articleId);
}
