package com.crawldata.repository;

import com.crawldata.model.CrawlerSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CrawlerSourceRepository extends JpaRepository<CrawlerSource, Long> {
    List<CrawlerSource> findByStatus(String status);
}
