import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-container">
      <div class="auth-card card">
        <h2>Đăng ký tài khoản</h2>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label>Tên đăng nhập</label>
            <input formControlName="username" type="text" placeholder="Ít nhất 3 ký tự">
          </div>
          <div class="field">
            <label>Email</label>
            <input formControlName="email" type="email" placeholder="email@example.com">
          </div>
          <div class="field">
            <label>Mật khẩu</label>
            <input formControlName="password" type="password" placeholder="Ít nhất 8 ký tự">
          </div>
          <div class="error" *ngIf="error">{{ error }}</div>
          <button type="submit" [disabled]="form.invalid || loading" class="btn-primary">
            {{ loading ? 'Đang đăng ký...' : 'Đăng ký' }}
          </button>
        </form>
        <p class="switch-link">Đã có tài khoản? <a routerLink="/auth/login">Đăng nhập</a></p>
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
export class RegisterComponent {
  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });
  loading = false;
  error   = '';

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    this.auth.register(this.form.value as any).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.error = err.error?.message || 'Đăng ký thất bại, thử lại sau.';
        this.loading = false;
      }
    });
  }
}
