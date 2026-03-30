export interface Article {
  id: number;
  title: string;
  sourceUrl: string;
  sourceName: string;
  imageUrl: string | null;
  author: string | null;
  publishedAt: string;
  status: 'RAW' | 'SUMMARIZED' | 'FAILED';
  categories: string[];
  summary?: Summary;
}

export interface Summary {
  summaryText: string;
  keyPoints: string | null;
  aiProvider: string;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface LoginRequest { username: string; password: string; }
export interface RegisterRequest { username: string; email: string; password: string; }
export interface AuthResponse { token: string; username: string; email: string; role: string; }
