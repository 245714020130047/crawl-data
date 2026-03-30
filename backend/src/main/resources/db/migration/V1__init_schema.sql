-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ==================== USERS ====================
CREATE TABLE users (
    id          SERIAL PRIMARY KEY,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email       VARCHAR(100) UNIQUE NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- USER, ADMIN
    preferences JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==================== CATEGORIES ====================
CREATE TABLE categories (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO categories (name) VALUES
    ('Thời sự'), ('Kinh tế'), ('Thế giới'), ('Xã hội'),
    ('Thể thao'), ('Giải trí'), ('Công nghệ'), ('Giáo dục'), ('Sức khỏe');

-- ==================== CRAWLER SOURCES ====================
CREATE TABLE crawler_sources (
    id                  SERIAL PRIMARY KEY,
    website_name        VARCHAR(100) NOT NULL,
    base_url            VARCHAR(500) NOT NULL,
    selector_config     JSONB        NOT NULL,  -- CSS selectors
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    retry_limit         INTEGER      NOT NULL DEFAULT 3,
    rate_limit_seconds  INTEGER      NOT NULL DEFAULT 5,
    created_by          INTEGER      REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial crawler sources
INSERT INTO crawler_sources (website_name, base_url, selector_config, enabled) VALUES
(
    'VnExpress',
    'https://vnexpress.net',
    '{
        "article_links": "article.item-news h3.title-news a",
        "title": "h1.title-detail",
        "content": "article.fck_detail p",
        "author": "p.author span",
        "published_date": "span.date",
        "category": "ul.breadcrumb li:last-child a",
        "image": "img.lazy"
    }',
    true
),
(
    'Tuổi Trẻ Online',
    'https://tuoitre.vn',
    '{
        "article_links": "h3.title-news a",
        "title": "h1.article-title",
        "content": "div.detail-content p",
        "author": "div.author-info strong",
        "published_date": "div.date-time",
        "category": "div.breadcrumb a:last-child",
        "image": "img.lazy"
    }',
    true
),
(
    'Thanh Niên',
    'https://thanhnien.vn',
    '{
        "article_links": "h3.story__heading a",
        "title": "h1.cms-title",
        "content": "div.detail-content div[data-role=content] p",
        "author": "div.cms-author strong",
        "published_date": "div.cms-date",
        "category": "div.breadcrumb a:last-child",
        "image": "img.cms-photo"
    }',
    true
);

-- ==================== ARTICLES ====================
CREATE TABLE articles (
    id              SERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    content         TEXT         NOT NULL,
    source_url      VARCHAR(1000) UNIQUE NOT NULL,
    source_site_id  INTEGER      REFERENCES crawler_sources(id) ON DELETE SET NULL,
    author          VARCHAR(200),
    published_date  TIMESTAMP,
    crawled_date    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    image_url       VARCHAR(500),
    category_id     INTEGER      REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_articles_category   ON articles(category_id);
CREATE INDEX idx_articles_crawled    ON articles(crawled_date DESC);
CREATE INDEX idx_articles_source     ON articles(source_site_id);
CREATE INDEX idx_articles_fts        ON articles
    USING GIN(to_tsvector('simple', title || ' ' || content));
CREATE INDEX idx_articles_title_trgm ON articles
    USING GIN(title gin_trgm_ops);

-- ==================== SUMMARIES ====================
CREATE TABLE summaries (
    id            SERIAL PRIMARY KEY,
    article_id    INTEGER       UNIQUE NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    summary_text  TEXT          NOT NULL,
    model_used    VARCHAR(50),
    tokens_used   INTEGER,
    cost_usd      DECIMAL(10,6),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==================== SEARCH EMBEDDINGS (pgvector) ====================
CREATE TABLE search_embeddings (
    article_id  INTEGER   PRIMARY KEY REFERENCES articles(id) ON DELETE CASCADE,
    embedding   vector(1536),  -- text-embedding-3-small dimension
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_embeddings_ivfflat
    ON search_embeddings USING ivfflat(embedding vector_cosine_ops)
    WITH (lists = 100);

-- ==================== READING HISTORY ====================
CREATE TABLE reading_history (
    id               SERIAL PRIMARY KEY,
    user_id          INTEGER   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id       INTEGER   NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    read_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_seconds INTEGER
);

CREATE INDEX idx_reading_user ON reading_history(user_id, read_at DESC);

-- ==================== BOOKMARKS ====================
CREATE TABLE bookmarks (
    id            SERIAL PRIMARY KEY,
    user_id       INTEGER   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id    INTEGER   NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    bookmarked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, article_id)
);

CREATE INDEX idx_bookmarks_user ON bookmarks(user_id);

-- ==================== CRAWL JOBS (monitoring) ====================
CREATE TABLE crawl_jobs (
    id              SERIAL PRIMARY KEY,
    source_id       INTEGER      REFERENCES crawler_sources(id) ON DELETE SET NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, SUCCESS, FAILED
    articles_count  INTEGER      DEFAULT 0,
    duplicate_count INTEGER      DEFAULT 0,
    error_message   TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_crawl_jobs_status  ON crawl_jobs(status, created_at DESC);
CREATE INDEX idx_crawl_jobs_source  ON crawl_jobs(source_id, created_at DESC);

-- ==================== SUMMARIZATION CONFIG ====================
CREATE TABLE summarization_config (
    id               SERIAL PRIMARY KEY,
    interval_seconds INTEGER     NOT NULL DEFAULT 300,  -- 5 minutes
    enabled          BOOLEAN     NOT NULL DEFAULT TRUE,
    max_length       INTEGER     NOT NULL DEFAULT 150,
    timeout_sec      INTEGER     NOT NULL DEFAULT 30,
    model            VARCHAR(50) NOT NULL DEFAULT 'gemini',  -- gemini, openai
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Default config row (singleton pattern)
INSERT INTO summarization_config (interval_seconds, enabled, max_length, model)
VALUES (300, true, 150, 'gemini');

-- ==================== ARTICLE-CATEGORY JUNCTION ====================
CREATE TABLE article_categories (
    article_id  INTEGER NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, category_id)
);
