import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Article, PagedResponse } from '../models/article.model';

@Injectable({ providedIn: 'root' })
export class ArticleService {

  constructor(private http: HttpClient) {}

  getLatest(page = 0, size = 20): Observable<PagedResponse<Article>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<Article>>(`${environment.apiUrl}/articles`, { params });
  }

  getById(id: number): Observable<Article> {
    return this.http.get<Article>(`${environment.apiUrl}/articles/${id}`);
  }

  getByCategory(categoryId: number, page = 0, size = 20): Observable<PagedResponse<Article>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<Article>>(
      `${environment.apiUrl}/articles/category/${categoryId}`, { params });
  }

  search(query: string, page = 0, size = 20): Observable<PagedResponse<Article>> {
    const params = new HttpParams().set('q', query).set('page', page).set('size', size);
    return this.http.get<PagedResponse<Article>>(`${environment.apiUrl}/search`, { params });
  }

  addBookmark(articleId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/user/bookmarks/${articleId}`, {});
  }

  removeBookmark(articleId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/user/bookmarks/${articleId}`);
  }

  getBookmarks(page = 0, size = 20): Observable<PagedResponse<Article>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<Article>>(`${environment.apiUrl}/user/bookmarks`, { params });
  }
}
