import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../core/services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="admin">
      <h2>⚙️ Admin Dashboard</h2>

      <!-- Stats -->
      <div class="stats-grid" *ngIf="stats()">
        <div class="stat-card card">
          <div class="stat-value">{{ stats().totalArticles | number }}</div>
          <div class="stat-label">Tổng bài viết</div>
        </div>
        <div class="stat-card card">
          <div class="stat-value">{{ stats().summarizedArticles | number }}</div>
          <div class="stat-label">Đã tóm tắt</div>
        </div>
        <div class="stat-card card">
          <div class="stat-value">{{ stats().rawArticles | number }}</div>
          <div class="stat-label">Chờ tóm tắt</div>
        </div>
        <div class="stat-card card">
          <div class="stat-value">{{ stats().activeSources }}</div>
          <div class="stat-label">Nguồn đang hoạt động</div>
        </div>
      </div>

      <!-- Action buttons -->
      <div class="actions">
        <button (click)="triggerCrawl()" class="btn-action" [disabled]="crawling()">
          {{ crawling() ? '⏳ Đang crawl...' : '🔄 Crawl tất cả nguồn' }}
        </button>
        <button (click)="triggerSummarize()" class="btn-action secondary" [disabled]="summarizing()">
          {{ summarizing() ? '⏳ Đang tóm tắt...' : '🤖 Tóm tắt ngay' }}
        </button>
        <a routerLink="/admin/sources" class="btn-action outline">📡 Quản lý nguồn</a>
      </div>

      <!-- Summarization config -->
      <div class="config-section card" *ngIf="config()">
        <h3>Cài đặt tóm tắt tự động</h3>
        <div class="config-row">
          <label>Kích hoạt:</label>
          <input type="checkbox" [(ngModel)]="config().isEnabled">
        </div>
        <div class="config-row">
          <label>AI Provider:</label>
          <select [(ngModel)]="config().provider">
            <option value="GEMINI">Gemini</option>
            <option value="OPENAI">OpenAI</option>
          </select>
        </div>
        <div class="config-row">
          <label>Batch size:</label>
          <input type="number" [(ngModel)]="config().batchSize" min="1" max="50">
        </div>
        <button (click)="saveConfig()" class="btn-save">Lưu cài đặt</button>
      </div>

      <!-- Recent jobs -->
      <div class="jobs-section" *ngIf="stats()?.recentJobs?.length">
        <h3>Lịch sử crawl gần đây</h3>
        <table class="jobs-table">
          <thead>
            <tr><th>ID</th><th>Nguồn</th><th>Trạng thái</th><th>Tìm thấy</th><th>Đã lưu</th><th>Trùng lặp</th><th>Thời gian</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let job of stats().recentJobs">
              <td>{{ job.id }}</td>
              <td>{{ job.source?.name }}</td>
              <td><span [class]="'status-' + job.status.toLowerCase()">{{ job.status }}</span></td>
              <td>{{ job.articlesFound }}</td>
              <td>{{ job.articlesSaved }}</td>
              <td>{{ job.duplicateCount }}</td>
              <td>{{ job.startedAt | date:'dd/MM HH:mm' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .admin { max-width: 1000px; margin: 0 auto; }
    h2 { font-size: 24px; margin-bottom: 20px; }
    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
    .stat-card { padding: 20px; text-align: center; }
    .stat-value { font-size: 32px; font-weight: 700; color: #1a73e8; }
    .stat-label { font-size: 13px; color: #888; margin-top: 4px; }
    .actions { display: flex; gap: 12px; margin-bottom: 24px; flex-wrap: wrap; }
    .btn-action { padding: 10px 20px; background: #1a73e8; color: white; border: none;
      border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 500; }
    .btn-action.secondary { background: #34a853; }
    .btn-action.outline { background: white; color: #1a73e8; border: 1px solid #1a73e8; }
    .btn-action:disabled { opacity: .5; cursor: default; }
    .config-section { padding: 20px; margin-bottom: 24px; }
    .config-section h3 { margin: 0 0 16px; }
    .config-row { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; }
    .config-row label { width: 120px; font-size: 14px; }
    .config-row input, .config-row select { padding: 6px 10px; border: 1px solid #ddd; border-radius: 4px; }
    .btn-save { padding: 8px 20px; background: #34a853; color: white; border: none; border-radius: 6px; cursor: pointer; }
    .jobs-table { width: 100%; border-collapse: collapse; font-size: 13px; }
    .jobs-table th, .jobs-table td { padding: 8px 12px; border-bottom: 1px solid #eee; text-align: left; }
    .jobs-table th { background: #f5f7fa; font-weight: 600; }
    .status-done { color: #34a853; } .status-failed { color: #ea4335; } .status-running { color: #fbbc04; }
  `]
})
export class AdminDashboardComponent implements OnInit {
  stats      = signal<any>(null);
  config     = signal<any>(null);
  crawling   = signal(false);
  summarizing = signal(false);

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.loadStats();
    this.adminService.getSummarizationConfig().subscribe(c => this.config.set(c));
  }

  loadStats() {
    this.adminService.getStats().subscribe(s => this.stats.set(s));
  }

  triggerCrawl() {
    this.crawling.set(true);
    this.adminService.triggerCrawl().subscribe({
      next: () => { this.crawling.set(false); this.loadStats(); },
      error: () => this.crawling.set(false)
    });
  }

  triggerSummarize() {
    this.summarizing.set(true);
    this.adminService.triggerSummarization().subscribe({
      next: () => { this.summarizing.set(false); this.loadStats(); },
      error: () => this.summarizing.set(false)
    });
  }

  saveConfig() {
    this.adminService.updateSummarizationConfig(this.config()).subscribe();
  }
}
