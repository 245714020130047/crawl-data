import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-sources',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="sources-page">
      <div class="page-header">
        <h2>📡 Quản lý nguồn crawl</h2>
        <button (click)="startAdd()" class="btn-add">+ Thêm nguồn</button>
      </div>

      <!-- Add / Edit form -->
      <div *ngIf="editing()" class="edit-form card">
        <h3>{{ form().id ? 'Chỉnh sửa' : 'Thêm mới' }} nguồn</h3>
        <div class="fields">
          <label>Tên nguồn <input [(ngModel)]="form().name" placeholder="VnExpress"></label>
          <label>Base URL <input [(ngModel)]="form().baseUrl" placeholder="https://vnexpress.net"></label>
          <label>RSS URL <input [(ngModel)]="form().rssUrl" placeholder="https://vnexpress.net/rss/tin-moi-nhat.rss"></label>
          <label>Trạng thái
            <select [(ngModel)]="form().status">
              <option value="ACTIVE">ACTIVE</option>
              <option value="PAUSED">PAUSED</option>
              <option value="DISABLED">DISABLED</option>
            </select>
          </label>
          <label>Crawl interval (phút) <input type="number" [(ngModel)]="form().crawlIntervalMinutes"></label>
          <label>Delay (ms) <input type="number" [(ngModel)]="form().requestDelayMs"></label>
        </div>
        <div class="form-actions">
          <button (click)="save()" class="btn-save">Lưu</button>
          <button (click)="editing.set(false)" class="btn-cancel">Hủy</button>
        </div>
      </div>

      <!-- Sources table -->
      <div *ngIf="loading()" class="loading-center"><div class="spinner"></div></div>
      <table class="sources-table" *ngIf="!loading()">
        <thead>
          <tr><th>Tên</th><th>RSS URL</th><th>Trạng thái</th><th>Interval</th><th>Lần cuối crawl</th><th>Hành động</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let src of sources()">
            <td>{{ src.name }}</td>
            <td><a [href]="src.rssUrl" target="_blank" class="rss-link">RSS</a></td>
            <td><span [class]="'status-' + src.status.toLowerCase()">{{ src.status }}</span></td>
            <td>{{ src.crawlIntervalMinutes }}m</td>
            <td>{{ src.lastCrawledAt | date:'dd/MM HH:mm' }}</td>
            <td class="actions">
              <button (click)="startEdit(src)">✏️</button>
              <button (click)="triggerCrawl(src.id)">▶️</button>
              <button (click)="deleteSource(src.id)" class="btn-delete">🗑️</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .sources-page { max-width: 1000px; margin: 0 auto; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .btn-add { padding: 8px 20px; background: #1a73e8; color: white; border: none; border-radius: 6px; cursor: pointer; }
    .edit-form { padding: 20px; margin-bottom: 24px; }
    .edit-form h3 { margin: 0 0 16px; }
    .fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .fields label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; font-weight: 500; }
    .fields input, .fields select { padding: 8px 10px; border: 1px solid #ddd; border-radius: 4px; }
    .form-actions { display: flex; gap: 10px; margin-top: 16px; }
    .btn-save { padding: 8px 20px; background: #34a853; color: white; border: none; border-radius: 6px; cursor: pointer; }
    .btn-cancel { padding: 8px 20px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 6px; cursor: pointer; }
    .sources-table { width: 100%; border-collapse: collapse; font-size: 14px; }
    .sources-table th, .sources-table td { padding: 10px 12px; border-bottom: 1px solid #eee; text-align: left; }
    .sources-table th { background: #f5f7fa; font-weight: 600; }
    .status-active { color: #34a853; font-weight: 600; } .status-paused { color: #fbbc04; } .status-disabled { color: #aaa; }
    .rss-link { color: #1a73e8; font-size: 12px; border: 1px solid #1a73e8; padding: 2px 8px; border-radius: 4px; }
    .actions { display: flex; gap: 6px; }
    .actions button { background: none; border: none; cursor: pointer; font-size: 16px; padding: 2px; }
    .btn-delete:hover { color: #ea4335; }
    .loading-center { display: flex; justify-content: center; padding: 40px; }
  `]
})
export class SourcesComponent implements OnInit {
  sources = signal<any[]>([]);
  loading = signal(true);
  editing = signal(false);
  form    = signal<any>({});

  constructor(private adminService: AdminService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.adminService.getSources().subscribe({
      next: s => { this.sources.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  startAdd() {
    this.form.set({ name: '', baseUrl: '', rssUrl: '', status: 'ACTIVE',
                    crawlIntervalMinutes: 60, requestDelayMs: 1000 });
    this.editing.set(true);
  }

  startEdit(src: any) { this.form.set({ ...src }); this.editing.set(true); }

  save() {
    const f = this.form();
    const obs = f.id
      ? this.adminService.updateSource(f.id, f)
      : this.adminService.createSource(f);
    obs.subscribe(() => { this.editing.set(false); this.load(); });
  }

  deleteSource(id: number) {
    if (confirm('Xác nhận xóa nguồn này?')) {
      this.adminService.deleteSource(id).subscribe(() => this.load());
    }
  }

  triggerCrawl(id: number) {
    this.adminService.triggerCrawl(id).subscribe(res => alert(res.message));
  }
}
