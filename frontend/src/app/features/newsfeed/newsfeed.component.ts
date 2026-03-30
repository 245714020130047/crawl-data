import { Component, OnInit, signal, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ArticleService } from '../../core/services/article.service';
import { Article, PagedResponse } from '../../core/models/article.model';

@Component({
  selector: 'app-newsfeed',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="newsfeed">
      <div *ngIf="loading()" class="loading-center">
        <div class="spinner"></div>
      </div>

      <div *ngIf="!loading()" class="article-grid">
        <div *ngFor="let article of articles()" class="card article-card">
          <a [routerLink]="['/article', article.id]">
            <img *ngIf="article.imageUrl" [src]="article.imageUrl" [alt]="article.title"
                 class="article-img" loading="lazy">
            <div class="article-info">
              <div class="article-meta">
                <span class="badge">{{ article.categories[0] || 'Tin tức' }}</span>
                <span class="source">{{ article.sourceName }}</span>
                <span class="date">{{ article.publishedAt | date:'dd/MM/yyyy HH:mm' }}</span>
              </div>
              <h3 class="article-title">{{ article.title }}</h3>
              <p *ngIf="article.summary" class="article-excerpt">
                {{ article.summary.summaryText | slice:0:150 }}...
              </p>
            </div>
          </a>
        </div>
      </div>

      <div *ngIf="!loading() && articles().length === 0" class="empty-state">
        Không có bài viết nào.
      </div>

      <div class="pagination" *ngIf="paged()">
        <button [disabled]="paged()!.page === 0" (click)="loadPage(paged()!.page - 1)">← Trước</button>
        <span>Trang {{ paged()!.page + 1 }} / {{ paged()!.totalPages }}</span>
        <button [disabled]="paged()!.last" (click)="loadPage(paged()!.page + 1)">Sau →</button>
      </div>
    </div>
  `,
  styles: [`
    .loading-center { display: flex; justify-content: center; padding: 60px; }
    .article-card img.article-img { width: 100%; height: 200px; object-fit: cover; }
    .article-info { padding: 16px; }
    .article-meta { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-bottom: 8px; }
    .source { font-size: 12px; color: #888; }
    .date { font-size: 12px; color: #aaa; margin-left: auto; }
    .article-title { font-size: 16px; font-weight: 600; line-height: 1.4; margin: 0 0 8px; color: #1a1a2e; }
    .article-excerpt { font-size: 13px; color: #666; line-height: 1.5; margin: 0; }
    .article-card a { display: block; }
    .article-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.12); transform: translateY(-2px); transition: all .2s; }
    .empty-state { text-align: center; padding: 60px; color: #aaa; }
    .pagination { display: flex; justify-content: center; gap: 16px; align-items: center; padding: 24px; }
    .pagination button { padding: 8px 20px; border: 1px solid #ddd; border-radius: 6px;
      background: white; cursor: pointer; }
    .pagination button:disabled { opacity: .4; cursor: default; }
  `]
})
export class NewsfeedComponent implements OnInit, OnChanges {
  @Input() id?: string;  // categoryId from route

  articles  = signal<Article[]>([]);
  paged     = signal<PagedResponse<Article> | null>(null);
  loading   = signal(true);

  constructor(private articleService: ArticleService, private route: ActivatedRoute) {}

  ngOnInit() { this.load(0); }

  ngOnChanges() { this.load(0); }

  load(page: number) {
    this.loading.set(true);
    const obs = this.id
      ? this.articleService.getByCategory(+this.id, page)
      : this.articleService.getLatest(page);

    obs.subscribe({
      next: (res) => {
        this.paged.set(res);
        this.articles.set(res.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  loadPage(page: number) { this.load(page); }
}
