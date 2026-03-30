import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private base = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  getStats(): Observable<any> {
    return this.http.get(`${this.base}/stats`);
  }

  getSources(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/sources`);
  }

  createSource(source: any): Observable<any> {
    return this.http.post(`${this.base}/sources`, source);
  }

  updateSource(id: number, source: any): Observable<any> {
    return this.http.put(`${this.base}/sources/${id}`, source);
  }

  deleteSource(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/sources/${id}`);
  }

  triggerCrawl(sourceId?: number): Observable<any> {
    const url = sourceId
      ? `${this.base}/crawl/trigger?sourceId=${sourceId}`
      : `${this.base}/crawl/trigger`;
    return this.http.post(url, {});
  }

  triggerSummarization(): Observable<any> {
    return this.http.post(`${this.base}/summarization/trigger`, {});
  }

  getSummarizationConfig(): Observable<any> {
    return this.http.get(`${this.base}/summarization/config`);
  }

  updateSummarizationConfig(config: any): Observable<any> {
    return this.http.put(`${this.base}/summarization/config`, config);
  }
}
