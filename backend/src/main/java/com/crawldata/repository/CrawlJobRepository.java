package com.crawldata.repository;

import com.crawldata.model.CrawlJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlJobRepository extends JpaRepository<CrawlJob, Long> {
    Page<CrawlJob> findByOrderByStartedAtDesc(Pageable pageable);
    Page<CrawlJob> findBySourceIdOrderByStartedAtDesc(Long sourceId, Pageable pageable);
    long countByStatus(String status);
}
