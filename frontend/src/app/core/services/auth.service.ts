import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/article.model';

interface CurrentUser { username: string; email: string; role: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY  = 'auth_user';

  private _token  = signal<string | null>(localStorage.getItem(this.TOKEN_KEY));
  private _user   = signal<CurrentUser | null>(this.loadUser());

  currentUser = this._user.asReadonly();
  isLoggedIn  = computed(() => !!this._token());
  isAdmin     = computed(() => this._user()?.role === 'ADMIN');

  constructor(private http: HttpClient, private router: Router) {}

  login(body: LoginRequest) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, body).pipe(
      tap(res => this.setSession(res))
    );
  }

  register(body: RegisterRequest) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, body).pipe(
      tap(res => this.setSession(res))
    );
  }

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this._token.set(null);
    this._user.set(null);
    this.router.navigate(['/']);
  }

  getToken(): string | null { return this._token(); }

  private setSession(res: AuthResponse) {
    localStorage.setItem(this.TOKEN_KEY, res.token);
    const user: CurrentUser = { username: res.username, email: res.email, role: res.role };
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this._token.set(res.token);
    this._user.set(user);
  }

  private loadUser(): CurrentUser | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
