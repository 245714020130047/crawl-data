import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ArticleService } from '../../core/services/article.service';
import { Article, PagedResponse } from '../../core/models/article.model';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="search-page">
      <div class="search-box">
        <input [(ngModel)]="query" placeholder="Tìm kiếm bài viết..."
               (keyup.enter)="search(0)" class="search-input">
        <button (click)="search(0)" class="search-btn">Tìm kiếm</button>
      </div>

      <div *ngIf="loading()" class="loading-center"><div class="spinner"></div></div>

      <div *ngIf="!loading() && searched()">
        <p class="result-count">{{ paged()?.totalElements }} kết quả</p>
        <div class="article-grid">
          <div *ngFor="let article of articles()" class="card article-card">
            <a [routerLink]="['/article', article.id]">
              <img *ngIf="article.imageUrl" [src]="article.imageUrl" [alt]="article.title"
                   class="article-img" loading="lazy">
              <div class="article-info">
                <h3>{{ article.title }}</h3>
                <p *ngIf="article.summary">{{ article.summary.summaryText | slice:0:120 }}...</p>
                <span class="date">{{ article.publishedAt | date:'dd/MM/yyyy' }}</span>
              </div>
            </a>
          </div>
        </div>
        <div *ngIf="articles().length === 0" class="empty-state">Không tìm thấy kết quả.</div>
      </div>
    </div>
  `,
  styles: [`
    .search-page { max-width: 900px; margin: 0 auto; }
    .search-box { display: flex; gap: 12px; margin-bottom: 24px; }
    .search-input { flex: 1; padding: 12px 16px; border: 1px solid #ddd; border-radius: 8px; font-size: 16px; }
    .search-btn { padding: 12px 24px; background: #1a73e8; color: white;
      border: none; border-radius: 8px; cursor: pointer; font-size: 16px; }
    .result-count { color: #666; margin-bottom: 16px; }
    .article-card img.article-img { width: 100%; height: 160px; object-fit: cover; }
    .article-info { padding: 12px; }
    .article-info h3 { font-size: 15px; margin: 0 0 6px; }
    .article-info p { font-size: 13px; color: #666; margin: 0 0 6px; }
    .date { font-size: 12px; color: #aaa; }
    .loading-center { display: flex; justify-content: center; padding: 40px; }
    .empty-state { text-align: center; padding: 40px; color: #aaa; }
  `]
})
export class SearchComponent {
  query   = '';
  articles = signal<Article[]>([]);
  paged    = signal<PagedResponse<Article> | null>(null);
  loading  = signal(false);
  searched = signal(false);

  constructor(private articleService: ArticleService) {}

  search(page: number) {
    if (!this.query.trim()) return;
    this.loading.set(true);
    this.articleService.search(this.query, page).subscribe({
      next: res => {
        this.paged.set(res);
        this.articles.set(res.content);
        this.loading.set(false);
        this.searched.set(true);
      },
      error: () => this.loading.set(false)
    });
  }
}
