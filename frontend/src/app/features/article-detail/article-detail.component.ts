import { Component, OnInit, signal, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ArticleService } from '../../core/services/article.service';
import { AuthService } from '../../core/services/auth.service';
import { Article } from '../../core/models/article.model';

@Component({
  selector: 'app-article-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div *ngIf="loading()" class="loading-center"><div class="spinner"></div></div>
    <div *ngIf="!loading() && article()" class="article-detail card">
      <div class="article-header">
        <div class="article-meta">
          <span class="badge">{{ article()!.categories[0] || 'Tin tức' }}</span>
          <a [href]="article()!.sourceUrl" target="_blank" class="source-link">
            {{ article()!.sourceName }}
          </a>
          <span class="date">{{ article()!.publishedAt | date:'dd/MM/yyyy HH:mm' }}</span>
        </div>
        <h1>{{ article()!.title }}</h1>
        <div class="author" *ngIf="article()!.author">✍️ {{ article()!.author }}</div>
        <button (click)="toggleBookmark()" class="bookmark-btn">
          {{ bookmarked() ? '🔖 Đã lưu' : '📌 Lưu bài' }}
        </button>
      </div>

      <img *ngIf="article()!.imageUrl" [src]="article()!.imageUrl" [alt]="article()!.title" class="hero-img">

      <!-- AI Summary block -->
      <div *ngIf="article()!.summary" class="summary-block">
        <h3>🤖 Tóm tắt AI ({{ article()!.summary!.aiProvider }})</h3>
        <p>{{ article()!.summary!.summaryText }}</p>
        <div *ngIf="article()!.summary!.keyPoints" class="key-points">
          <strong>Điểm chính:</strong>
          <pre>{{ article()!.summary!.keyPoints }}</pre>
        </div>
      </div>

      <div class="article-content" [innerHTML]="article()!.content ?? ''"></div>

      <div class="article-footer">
        <a [href]="article()!.sourceUrl" target="_blank" class="btn-read-original">
          Đọc bài gốc →
        </a>
        <a routerLink="/">← Về trang chủ</a>
      </div>
    </div>
  `,
  styles: [`
    .loading-center { display: flex; justify-content: center; padding: 60px; }
    .article-detail { max-width: 800px; margin: 0 auto; padding: 32px; }
    .article-header { margin-bottom: 24px; }
    .article-meta { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
    .source-link { font-size: 13px; color: #1a73e8; }
    .date { font-size: 13px; color: #aaa; }
    .author { font-size: 14px; color: #666; margin: 8px 0; }
    h1 { font-size: 26px; line-height: 1.4; margin: 8px 0; }
    .bookmark-btn { border: 1px solid #1a73e8; background: white; color: #1a73e8;
      border-radius: 6px; padding: 6px 16px; cursor: pointer; margin-top: 8px; }
    .hero-img { width: 100%; max-height: 400px; object-fit: cover; border-radius: 8px; margin-bottom: 24px; }
    .summary-block { background: #f0f7ff; border-left: 4px solid #1a73e8;
      padding: 16px 20px; border-radius: 8px; margin-bottom: 24px; }
    .summary-block h3 { margin: 0 0 8px; font-size: 16px; }
    .summary-block p { margin: 0; line-height: 1.6; }
    .key-points { margin-top: 12px; font-size: 14px; }
    .key-points pre { white-space: pre-wrap; font-family: inherit; margin: 4px 0 0; }
    .article-content { line-height: 1.8; font-size: 16px; color: #333; }
    .article-footer { display: flex; gap: 16px; align-items: center; margin-top: 32px; padding-top: 16px;
      border-top: 1px solid #eee; }
    .btn-read-original { background: #1a73e8; color: white; padding: 10px 20px;
      border-radius: 6px; font-weight: 500; }
  `]
})
export class ArticleDetailComponent implements OnInit {
  @Input() id!: string;

  article   = signal<Article | null>(null);
  loading   = signal(true);
  bookmarked = signal(false);

  constructor(private articleService: ArticleService, public auth: AuthService) {}

  ngOnInit() {
    this.articleService.getById(+this.id).subscribe({
      next: a => { this.article.set(a); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  toggleBookmark() {
    if (!this.auth.isLoggedIn()) return;
    const id = this.article()!.id;
    if (this.bookmarked()) {
      this.articleService.removeBookmark(id).subscribe(() => this.bookmarked.set(false));
    } else {
      this.articleService.addBookmark(id).subscribe(() => this.bookmarked.set(true));
    }
  }
}
