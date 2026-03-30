# Plan: Vietnamese News Crawler + AI Summarization System (UPDATED)

## TL;DR
Build a distributed news crawling system with:
- **Hourly updates** from configurable Vietnamese news sites (stored in DB table)
- **RabbitMQ** for async job queue (crawl jobs & summarize jobs, retry, DLQ)
- **PostgreSQL** for articles, users, categories, search embeddings
- **Redis** for cache, duplicate detection, rate limiting (not job queue)
- **Configurable AI summarization** (interval-based or on-demand via Gemini/OpenAI)
- **Hybrid search**: Full-text + semantic similarity (pgvector)
- **Admin dashboard** with crawl monitoring, error alerts, manual controls
- **Angular UI** with user accounts, bookmarks, reading history, export
- **Medium-scale distributed architecture** ready for multiple crawlers

---

## Implementation Phases (7 phases, ordered by dependencies)

### Phase 1: Foundation Setup (Non-blocking)
**Goals**: Project scaffolding, Docker environment, database schema

1. **Spring Boot Backend** (Java 17+, Spring Boot 3.x, Maven)
   - Spring Web MVC, Spring Data JPA, Spring Security
   - OpenAPI/Swagger for REST documentation
   - Logging: SLF4J + Logback with separate crawl logs

2. **Angular Frontend** (Angular 17+, TypeScript)
   - Bootstrap or Angular Material for UI
   - Responsive design (mobile-first)

3. **PostgreSQL Schema** (Flyway migrations)
   - `users`, `articles`, `summaries`, `reading_history`, `bookmarks`
   - `categories`, `article_categories` (junction table)
   - `crawler_sources` (configurable crawlers)
   - `crawl_jobs`, `summarization_config` (monitoring)
   - `search_embeddings` (pgvector for semantic search)

4. **Docker Compose Stack**
   - PostgreSQL 15+ with pgvector extension
   - Redis 7+ (RDB + AOF persistence)
   - RabbitMQ 3+ with management plugin (port 5672, UI 15672)
   - Spring Boot backend container
   - Angular frontend (Nginx)
   - Optional: separate crawler-worker container

5. **Redis Configuration** (cache + dedup only, not job queue)
   - RDB snapshots every 900s
   - AOF (fsync every second)
   - Maxmemory policy: allkeys-lru (evict oldest accessed keys)
   - Persist duplicate cache across restarts

---

### Phase 2: Backend API Layer (depends on Phase 1)
**Goals**: User authentication, REST endpoints, database access

1. **JPA Entities & Repositories**
   - User, Article, Summary, ReadingHistory, Bookmark, Category
   - Custom queries for search, filtering, analytics
   - Indexes on: title, source_url, crawled_date, category_id

2. **Authentication & Authorization** (JWT)
   - User registration/login endpoints
   - JWT token generation (exp: 24 hours)
   - Role-based access: USER, ADMIN
   - Password hashing: BCrypt

3. **Core REST Endpoints**
   - **Articles**: GET `/api/articles` (paginated, filtered by category, date)
   - **Search**: GET `/api/search?query=...&category=...` (full-text)
   - **Semantic search**: GET `/api/articles/related/{id}` (pgvector similarity)
   - **Summaries**: GET `/api/articles/{id}/summary`
   - **Bookmarks**: GET/POST/DELETE `/api/bookmarks/{id}`
   - **Reading history**: GET `/api/user/history` (paginated)
   - **Export**: POST `/api/user/export` (PDF/CSV)
   - **User**: GET/PUT `/api/user/profile`

4. **Caching Strategy** (HTTP interceptors + Redis)
   - Cache article list (TTL: 30 min)
   - Cache search results (TTL: 1 hour)
   - Invalidate on new article crawl

---

### Phase 3: Configurable Crawler Infrastructure (depends on Phase 1)
**Goals**: Flexible, database-driven crawler configuration; RabbitMQ message-driven crawling

1. **Crawler Sources Management** (DB-driven)
   - `crawler_sources` table: (id, website_name, base_url, selector_config_json, enabled, retry_limit)
   - Selector config example:
     ```json
     {
       "title": "h1.article-title",
       "content": "div.article-body p",
       "author": "span.author-name",
       "published_date": "time.pub-date",
       "category": "a.category"
     }
     ```
   - Admin API to add/edit/delete sources without code redeployment

2. **Generic HTML Parser** (Jsoup)
   - Extract structured fields using CSS selectors
   - Handle edge cases: missing fields, malformed HTML
   - Custom parsers for complex sites (VnExpress, Tuổi Trẻ, etc.)

3. **RabbitMQ Job Queue** (crawl orchestration)
   - Exchange: `crawl.exchange` (topic type)
   - Queue: `crawl.job.queue` → routing key `crawl.source.*`
   - Retry queue: `crawl.retry.queue` (x-message-ttl: 60s, reroute to main queue)
   - Dead letter queue: `crawl.dlq` (after 3 retries → alert)
   - Job payload: `{source_id, website_name, base_url, scheduled_at, retry_count}`
   - Scheduler runs hourly: fetch enabled sources → publish to `crawl.exchange`

4. **Crawler Worker** (can be separate Docker service)
   - `@RabbitListener` on `crawl.job.queue`
   - Execute crawl logic: fetch HTML → parse → extract data
   - Check Redis duplicate cache before saving to DB
   - On failure: nack message → routed to retry → then DLQ

5. **Rate Limiting per Source** (Redis)
   - Counter key: `rate_limit:{source_id}` (incremented per crawl)
   - TTL: 24 hours
   - Respect robots.txt, add delays between requests

6. **Duplicate Detection** (Redis + DB fallback)
   - Hash URL: `sha256(source_id || url)`
   - Check Redis set: `crawled_urls:{date}`
   - If hit → skip saving (don't duplicate)
   - If miss → save to DB, add to Redis cache (TTL: 24h)
   - Fallback: if Redis down, check PostgreSQL unique constraint

---

### Phase 4: AI Summarization with Flexible Scheduling (depends on Phase 2, Phase 3)
**Goals**: Batch summarization with configurable timing, on-demand triggers

1. **AI Service Abstraction** (Strategy Pattern)
   - Interface: `SummarizationService`
   - Implementations: `GeminiSummarizationService`, `OpenAISummarizationService`
   - Switch providers without changing code

2. **Summarization Configuration** (Database-driven)
   - Table: `summarization_config` (interval_seconds, enabled, max_length, model, timeout)
   - Admin API: PUT `/api/admin/summarization/config` to update
   - Enable/disable toggle

3. **Batch Job Scheduler** (Configurable)
   - @Scheduled task runs every N minutes (configurable: 5, 10, 30, custom)
   - Fetch unsummarized articles (batch size: 10-20)
   - Publish each article to RabbitMQ `summarize.job.queue`
   - Worker processes and calls AI API with Vietnamese prompt
   - Store results: `summaries` table (id, article_id, summary_text, model, tokens_used)
   - Handle partial failures: retry queue → DLQ after 3 failures

4. **On-Demand Summarization**
   - Endpoint: POST `/api/admin/summarization/trigger`
   - Summarize all pending articles immediately
   - Return count of summarized articles

5. **Rate Limiting & Caching**
   - Track API costs via Redis: `ai_cost:{date}`
   - Cache in-progress summarizations: `summarizing:{article_id}` (prevent duplicate calls)
   - Respect API rate limits (Gemini: 60 req/min, OpenAI: depends on plan)

6. **Vietnamese-Aware Prompts**
   - Request: preserve key entities, dates, locations
   - Format: 2-3 sentences or bullet points (configurable)
   - Target length: 100-200 words
   - Example: "Tóm tắt bài viết sau bằng tiếng Việt, giữ lại các sự kiện quan trọng, ngày tháng, địa điểm..."

---

### Phase 5: Search & Analytics (depends on Phase 2, Phase 4)
**Goals**: Full-text search, semantic similarity, admin monitoring

1. **Full-Text Search** (PostgreSQL `tsvector`)
   - Create index: `CREATE INDEX idx_articles_fts ON articles USING GIN(to_tsvector('vietnamese', title || ' ' || content))`
   - Endpoint: GET `/api/search?q=...&category=...&limit=20&offset=0`
   - Results cached in Redis (TTL: 1 hour)

2. **Semantic Similarity Search** (pgvector)
   - During summarization: generate embedding via AI (OpenAI embedding model)
   - Store in `search_embeddings` table (article_id, embedding)
   - Endpoint: GET `/api/articles/{id}/related` → find similar articles via cosine distance
   - Combine with full-text results for hybrid search

3. **Analytics & Monitoring**
   - Track: article impressions, bookmarks, reading time
   - Category trends: count articles per day per category
   - Crawl metrics: success/failure rate per source, avg crawl time
   - Summarization metrics: cost, success rate, avg summary quality

---

### Phase 5b: Admin Dashboard & Control Panel (depends on Phase 2, Phase 4)
**Goals**: Real-time monitoring, configuration, error management

1. **Admin UI Components** (Angular)
   - **Crawl status**: Active jobs, last run time, success/failure counts per source
   - **Error alerts**: Failed crawls, API quota warnings, timeouts
   - **Manual controls**: Trigger crawl/summarization, enable/disable sources
   - **Crawler sources management**: Add/edit/delete sources with selector config editor
   - **Summarization settings**: Set interval, toggle enabled, select AI model
   - **Analytics**: Trends (article count, category distribution, search patterns)

2. **Admin REST Endpoints**
   - GET `/api/admin/status` (system health: DB, Redis, API quota)
   - GET `/api/admin/crawl-status` (job queue info, recent jobs, statistics)
   - GET `/api/admin/crawler-sources` (list all sources)
   - POST `/api/admin/crawler-sources` (create/edit/delete)
   - POST `/api/admin/crawl/trigger?source_id=123` (manual crawl)
   - GET `/api/admin/alerts` (failed jobs, warnings)
   - PUT `/api/admin/summarization/config` (update schedule/model)
   - POST `/api/admin/summarization/trigger` (on-demand)
   - GET `/api/admin/analytics` (dashboard data: trends, metrics)

---

### Phase 6: Angular Frontend & User Experience (depends on Phase 2)
**Goals**: User-friendly news interface, auth, bookmarks, admin tools

1. **Project Structure**
   - **Core module**: Shared services, HTTP interceptors, auth guards
   - **Feature modules**: News, Search, User profile, Admin panel
   - **Shared**: UI components (card, pagination, filters)

2. **Pages & Components**
   - **Newsfeed** (`/news`): List articles with infinite scroll, category filter, bookmarks icon
   - **Article Detail** (`/article/:id`): Full article + AI summary (side-by-side), related articles
   - **Search** (`/search?q=...`): Full-text results, category filters, semantic similarity toggle
   - **User Profile** (`/profile`): Reading history, bookmarks, preferences, export articles
   - **Export** (`/export`): Select articles → download as PDF/CSV
   - **Login/Signup** (`/auth`): Form-based, JWT token in localStorage
   - **Admin Dashboard** (`/admin`): Real-time crawl status, alerts, crawler management, settings
   - **Admin Crawler Sources** (`/admin/crawlers`): Add/edit sources with visual selector config editor

3. **State Management**
   - Use Angular Signals (17+) for reactive state
   - Services: ArticleService, AuthService, SearchService, AdminService
   - HTTP caching via interceptors

4. **UI/UX**
   - Responsive design: mobile, tablet, desktop
   - Dark mode toggle (save preference in user profile)
   - Infinite scroll on newsfeed
   - Search suggestions (autocomplete from recent searches)
   - Loading skeletons instead of spinners

---

### Phase 7: Containerization & Deployment
**Goals**: Local development + production-ready Docker setup

1. **Docker Composition**
   - `docker-compose.yml`:
     - PostgreSQL 15 (port 5432, volume for data)
     - Redis 7 (port 6379, RDB + AOF config)
     - Spring Boot backend (port 8080, JAR file or gradle bootRun)
     - Angular frontend (port 80, Nginx reverse proxy)
     - Optional: separate crawler worker service

2. **Backend Dockerfile** (multi-stage)
   - Stage 1: Build (Maven compile, package)
   - Stage 2: Runtime (JRE 17-slim, expose 8080, health check)

3. **Frontend Dockerfile** (Nginx)
   - Build Angular (ng build --prod)
   - Nginx conf: rewrite rules for SPA routing, gzip compression

4. **Environment Configuration**
   - `application-dev.yml`: localhost connections
   - `application-prod.yml`: production settings (RDS endpoint, managed Redis, etc.)
   - `.env` file: API keys (GEMINI_KEY, OPENAI_KEY), database credentials
   - Do NOT commit .env to git (add to .gitignore)

5. **Deployment Options** (Phase 2+)
   - **Local**: docker-compose up --build
   - **Staging**: Docker Swarm or simple VM
   - **Production**: Kubernetes (rolling updates, auto-scaling)

---

## Database Schema (PostgreSQL)

```sql
-- Users
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  role VARCHAR(20) DEFAULT 'USER', -- USER, ADMIN
  preferences JSONB DEFAULT '{}', -- dark_mode, language, etc.
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Articles
CREATE TABLE articles (
  id SERIAL PRIMARY KEY,
  title VARCHAR(500) NOT NULL,
  content TEXT NOT NULL,
  source_url VARCHAR(1000) UNIQUE NOT NULL,
  source_site_id INTEGER REFERENCES crawler_sources(id),
  author VARCHAR(200),
  published_date TIMESTAMP,
  crawled_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  image_url VARCHAR(500),
  category_id INTEGER REFERENCES categories(id)
);
CREATE INDEX idx_articles_fts ON articles USING GIN(
  to_tsvector('vietnamese', title || ' ' || content)
);
CREATE INDEX idx_articles_category ON articles(category_id);
CREATE INDEX idx_articles_crawled ON articles(crawled_date DESC);

-- AI Summaries
CREATE TABLE summaries (
  id SERIAL PRIMARY KEY,
  article_id INTEGER UNIQUE REFERENCES articles(id) ON DELETE CASCADE,
  summary_text TEXT NOT NULL,
  model_used VARCHAR(50), -- 'gemini', 'openai'
  tokens_used INTEGER,
  cost_usd DECIMAL(10,6),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Reading History
CREATE TABLE reading_history (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
  article_id INTEGER REFERENCES articles(id) ON DELETE CASCADE,
  read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  duration_seconds INTEGER
);
CREATE INDEX idx_reading_user ON reading_history(user_id, read_at DESC);

-- Bookmarks
CREATE TABLE bookmarks (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
  article_id INTEGER REFERENCES articles(id) ON DELETE CASCADE,
  bookmarked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, article_id)
);

-- Categories
CREATE TABLE categories (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) UNIQUE NOT NULL,
  description VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crawler Sources (configurable)
CREATE TABLE crawler_sources (
  id SERIAL PRIMARY KEY,
  website_name VARCHAR(100) NOT NULL,
  base_url VARCHAR(500) NOT NULL,
  selector_config JSONB NOT NULL, -- CSS selectors for parsing
  enabled BOOLEAN DEFAULT TRUE,
  retry_limit INTEGER DEFAULT 3,
  rate_limit_seconds INTEGER DEFAULT 5,
  created_by INTEGER REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crawl Jobs (monitoring)
CREATE TABLE crawl_jobs (
  id SERIAL PRIMARY KEY,
  source_id INTEGER REFERENCES crawler_sources(id),
  status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, SUCCESS, FAILED
  articles_count INTEGER,
  error_message TEXT,
  started_at TIMESTAMP,
  completed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_crawl_jobs_status ON crawl_jobs(status, created_at DESC);

-- Summarization Config (configurable)
CREATE TABLE summarization_config (
  id SERIAL PRIMARY KEY,
  interval_seconds INTEGER DEFAULT 300, -- 5 minutes
  enabled BOOLEAN DEFAULT TRUE,
  max_length INTEGER DEFAULT 150,
  timeout_sec INTEGER DEFAULT 30,
  model VARCHAR(50) DEFAULT 'gemini', -- gemini, openai
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Search Embeddings (pgvector)
CREATE TABLE search_embeddings (
  article_id INTEGER PRIMARY KEY REFERENCES articles(id) ON DELETE CASCADE,
  embedding vector(1536), -- OpenAI embedding size
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_search_embeddings ON search_embeddings USING ivfflat(embedding vector_cosine_ops);
```

---

## RabbitMQ Schema

```
# Crawl Pipeline
Exchange: crawl.exchange (topic)
  Queue: crawl.job.queue        → routing key: crawl.source.*
  Queue: crawl.retry.queue      → TTL 60s, DLX → crawl.exchange
  Queue: crawl.dlq              → dead-letter (manual review / alerts)

# Summarize Pipeline
Exchange: summarize.exchange (topic)
  Queue: summarize.job.queue    → routing key: summarize.article
  Queue: summarize.retry.queue  → TTL 30s, DLX → summarize.exchange
  Queue: summarize.dlq          → dead-letter (quota exceeded, API error)

# Message Payload Examples
Crawl job:     {source_id, website_name, base_url, selector_config, retry_count}
Summarize job: {article_id, title, content, model, max_length}
```

---

## Redis Schema (cache + dedup only)

```
# Duplicate Detection (daily rolling)
Key: crawled_urls:2024-03-25 (Set)
  → Members: hash(source_id||url)
  → TTL: 24h

# Crawl Rate Limiting
Key: rate_limit:{source_id} (Counter)
  → Value: number of crawls today
  → TTL: 24h
  → Used to enforce rate limits

# Article Cache
Key: articles:category:{cat_id} (String, JSON)
  → Value: JSON array of articles in category
  → TTL: 30 minutes

# Search Results Cache
Key: search:{query_hash} (String, JSON)
  → Value: search results
  → TTL: 1 hour

# Summarization In-Progress
Key: summarizing:{article_id} (Flag)
  → TTL: 5 minutes
  → Prevents duplicate summarization calls within batch window

# AI Cost Tracking
Key: ai_cost:{date} (Counter)
  → Value: cumulative cost in USD
  → TTL: 30 days
```

---

## Critical Architecture Decisions

### 1. **RabbitMQ as Job Queue** (not Redis Lists)
- **Pro**: Native retry, dead-letter, message acknowledgment, management UI
- **Con**: One more service to operate (addressed by Docker Compose + management UI)
- Topic exchange for flexible routing by source_id
- Retry policy: 3 attempts → DLQ → admin alert

### 2. **Duplicate Detection Strategy**
- **Fast path**: Check Redis set `crawled_urls:{date}` (< 1ms)
- **Fallback**: PostgreSQL unique constraint on source_url + date
- **Action**: Skip DB save if duplicate (not insert marked as duplicate)

### 3. **Batch Summarization Timing**
- **Configurable interval**: Admin sets in DB (5, 10, 30 min, or custom)
- **On-demand trigger**: Admin can immediately summarize pending articles
- **Smart batching**: Process 10-20 articles per batch to optimize API usage

### 4. **Search: Full-Text First, Semantic Optional**
- Full-text search is primary (PostgreSQL tsvector)
- Semantic search (pgvector) as bonus feature
- Toggle in UI: "Show similar articles"

### 5. **Admin Dashboard as First-Class Feature**
- Crawl monitoring, error alerts, manual controls
- Crawler source management (no code redeploy needed)
- Summarization schedule tweaking

---

## Key Tech Stack

| Component | Technology | Rationale |
|-----------|------------|-----------|
| Backend API | Spring Boot 3.x | Mature, widely supported |
| Job Queue | RabbitMQ 3+ | Reliable async, native retry, DLQ, management UI |
| Cache + Dedup | Redis 7+ | Fast reads, duplicate fingerprinting, rate limit |
| Database | PostgreSQL 15+ | Relational + pgvector (semantic search) |
| Parsing | Jsoup | Good CSS selector support |
| Frontend | Angular 17+ | Signals for state, TypeScript |
| AI | Gemini/OpenAI APIs | Flexible, free tier options |
| Deployment | Docker Compose | Local dev + production |
| Search | pg_trgm + tsvector | Native PostgreSQL FTS |

---

## Verification Steps & Testing Strategy

### Unit Tests
- Crawler selector parser: test CSS extraction with mock HTML
- SummarizationService: mock Gemini/OpenAI API calls
- API endpoints: MockMvc with in-memory DB
- Redis duplicate detection: Mock Redis

### Integration Tests
- End-to-end: crawl job published → RabbitMQ consumer → Redis dedup → save to DB → summarize job published → summary saved → retrieve via API
- RabbitMQ: verify retry queue TTL, DLQ routing after 3 failures
- Admin endpoints: add/edit crawler source → trigger crawl → verify queue message published
- Search: index articles → execute full-text query
- Auth: login → JWT token → authenticated requests

### Manual/Functional Tests
- Local docker-compose stack
- Trigger hourly crawl manually
- Verify articles appear (no duplicates)
- Verify summaries generated (test free tier first)
- Search: enter query → results appear with highlighting
- User signup/login/bookmark flow
- Admin: add new crawler source, disable it, check alerts

### Performance Tests  
- Load test: 1K articles/hour through pipeline
- Query performance: <500ms for typical search
- API response time: <200ms for article list
- Redis operations: < 5ms for duplicate check

---

## Decisions & Scope

**Included**:
- ✅ Hourly crawling from configurable sources (no code changes)
- ✅ Redis duplicate detection (fast, reliable)
- ✅ Configurable batch summarization (5/10/30 min intervals)
- ✅ On-demand summarization trigger
- ✅ User auth, reading history, bookmarks
- ✅ Full-text + semantic search (hybrid)
- ✅ Export articles (PDF/CSV)
- ✅ Admin dashboard with crawl monitoring, error alerts, manual controls
- ✅ Medium-scale distributed architecture (RabbitMQ queue + Redis cache)

**Excluded (Phase 2+)**:
- ❌ Real-time crawling (hourly is standard for news)
- ❌ Advanced NLP preprocessing (AI handles this)
- ❌ Recommendation engine (future enhancement)
- ❌ Mobile native apps (responsive web is sufficient)
- ❌ Kubernetes deployment (docker-compose + scaling horizontally)

**Assumptions**:
- API keys (Gemini, OpenAI) stored in environment variables (not in code)
- Free tier API usage (can upgrade later)
- Single PostgreSQL + single Redis (upgrade to Sentinel/Replication in production)

