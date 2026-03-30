import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-container">
      <div class="auth-card card">
        <h2>Đăng nhập</h2>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label>Tên đăng nhập</label>
            <input formControlName="username" type="text" placeholder="Username">
          </div>
          <div class="field">
            <label>Mật khẩu</label>
            <input formControlName="password" type="password" placeholder="Mật khẩu">
          </div>
          <div class="error" *ngIf="error">{{ error }}</div>
          <button type="submit" [disabled]="form.invalid || loading" class="btn-primary">
            {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
          </button>
        </form>
        <p class="switch-link">Chưa có tài khoản? <a routerLink="/auth/register">Đăng ký</a></p>
      </div>
    </div>
  `,
  styles: [`
    .auth-container { display: flex; justify-content: center; padding: 60px 16px; }
    .auth-card { width: 100%; max-width: 400px; padding: 32px; }
    h2 { margin: 0 0 24px; font-size: 22px; }
    .field { margin-bottom: 16px; }
    .field label { display: block; font-size: 14px; font-weight: 500; margin-bottom: 6px; }
    .field input { width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 6px; font-size: 15px; }
    .error { color: #d32f2f; font-size: 13px; margin-bottom: 12px; }
    .btn-primary { width: 100%; padding: 12px; background: #1a73e8; color: white;
      border: none; border-radius: 6px; font-size: 15px; cursor: pointer; }
    .btn-primary:disabled { opacity: .6; cursor: default; }
    .switch-link { text-align: center; margin-top: 16px; font-size: 14px; }
    .switch-link a { color: #1a73e8; }
  `]
})
export class LoginComponent {
  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });
  loading = false;
  error   = '';

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    this.auth.login(this.form.value as any).subscribe({
      next: () => this.router.navigate(['/']),
      error: () => {
        this.error = 'Tên đăng nhập hoặc mật khẩu không đúng.';
        this.loading = false;
      }
    });
  }
}
