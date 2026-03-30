package com.crawldata.ai;

import com.crawldata.model.Article;

public interface SummarizationService {
    /**
     * Summarize the given article, persist the summary, and update the article status.
     * Implementations must be thread-safe.
     */
    void summarize(Article article);
}
