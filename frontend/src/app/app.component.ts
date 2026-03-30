import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { CategoryService } from './core/services/category.service';
import { Category } from './core/models/article.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <header class="header">
      <div class="container header-inner">
        <a routerLink="/" class="logo">📰 CrawlData</a>
        <nav class="nav-categories">
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Tất cả</a>
          <a *ngFor="let cat of categories"
             [routerLink]="['/category', cat.id]"
             routerLinkActive="active">{{ cat.name }}</a>
        </nav>
        <div class="nav-actions">
          <a routerLink="/search">🔍</a>
          <ng-container *ngIf="auth.isLoggedIn(); else loginBtn">
            <a routerLink="/profile">{{ auth.currentUser()?.username }}</a>
            <a routerLink="/admin" *ngIf="auth.isAdmin()">Admin</a>
            <button (click)="auth.logout()">Đăng xuất</button>
          </ng-container>
          <ng-template #loginBtn>
            <a routerLink="/auth/login">Đăng nhập</a>
          </ng-template>
        </div>
      </div>
    </header>
    <main class="container">
      <router-outlet />
    </main>
    <footer class="footer">
      <div class="container">© 2024 CrawlData — Tin tức Việt Nam tổng hợp</div>
    </footer>
  `,
  styles: [`
    .header { background: white; box-shadow: 0 1px 4px rgba(0,0,0,.1); position: sticky; top: 0; z-index: 100; }
    .header-inner { display: flex; align-items: center; gap: 24px; padding: 12px 16px; }
    .logo { font-size: 20px; font-weight: 700; }
    .nav-categories { display: flex; gap: 16px; flex: 1; overflow-x: auto; }
    .nav-categories a { white-space: nowrap; font-size: 14px; color: #555; padding: 4px 0; }
    .nav-categories a.active { color: #1a73e8; font-weight: 600; border-bottom: 2px solid #1a73e8; }
    .nav-actions { display: flex; gap: 12px; align-items: center; font-size: 14px; }
    .nav-actions button { border: none; background: none; cursor: pointer; color: #d32f2f; }
    main { padding: 24px 16px; min-height: calc(100vh - 120px); }
    .footer { background: #1a1a2e; color: #aaa; text-align: center; padding: 16px; font-size: 13px; margin-top: 40px; }
  `]
})
export class AppComponent implements OnInit {
  categories: Category[] = [];

  constructor(public auth: AuthService, private categoryService: CategoryService) {}

  ngOnInit() {
    this.categoryService.getAll().subscribe(cats => this.categories = cats);
  }
}
