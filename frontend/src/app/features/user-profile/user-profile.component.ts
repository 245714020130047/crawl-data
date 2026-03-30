import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ArticleService } from '../../core/services/article.service';
import { Article } from '../../core/models/article.model';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="profile-page">
      <h2>👤 {{ auth.currentUser()?.username }}</h2>
      <p class="email">{{ auth.currentUser()?.email }}</p>

      <h3>🔖 Bài viết đã lưu</h3>
      <div *ngIf="loading()" class="loading-center"><div class="spinner"></div></div>
      <div class="article-grid" *ngIf="!loading()">
        <div *ngFor="let article of bookmarks()" class="card article-card">
          <a [routerLink]="['/article', article.id]">
            <img *ngIf="article.imageUrl" [src]="article.imageUrl" [alt]="article.title" class="article-img">
            <div class="article-info">
              <h4>{{ article.title }}</h4>
              <span class="date">{{ article.publishedAt | date:'dd/MM/yyyy' }}</span>
            </div>
          </a>
        </div>
      </div>
      <div *ngIf="!loading() && bookmarks().length === 0" class="empty-state">
        Chưa có bài viết nào được lưu.
      </div>
    </div>
  `,
  styles: [`
    .profile-page { max-width: 900px; margin: 0 auto; }
    h2 { font-size: 24px; margin-bottom: 4px; }
    .email { color: #888; margin-bottom: 32px; }
    .article-card img.article-img { width: 100%; height: 160px; object-fit: cover; }
    .article-info { padding: 12px; }
    .article-info h4 { font-size: 14px; margin: 0 0 6px; }
    .date { font-size: 12px; color: #aaa; }
    .loading-center { display: flex; justify-content: center; padding: 40px; }
    .empty-state { color: #aaa; padding: 20px 0; }
  `]
})
export class UserProfileComponent implements OnInit {
  bookmarks = signal<Article[]>([]);
  loading   = signal(true);

  constructor(public auth: AuthService, private articleService: ArticleService) {}

  ngOnInit() {
    this.articleService.getBookmarks().subscribe({
      next: res => { this.bookmarks.set(res.content); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
