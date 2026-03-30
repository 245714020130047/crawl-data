package com.crawldata.repository;

import com.crawldata.model.SummarizationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SummarizationConfigRepository extends JpaRepository<SummarizationConfig, Long> {
    Optional<SummarizationConfig> findFirstByOrderByIdAsc();
}
