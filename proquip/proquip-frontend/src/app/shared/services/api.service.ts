import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '@env/environment';

/**
 * ベースAPIサービス
 * HttpClientをラップし、共通のAPI呼び出し機能を提供する
 *
 * 技術的負債: エラーハンドリングが一貫していない
 * 一部のメソッドはcatchしてrethrowし、他のメソッドはそのまま流している
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {

  /** APIベースURL */
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * GETリクエスト
   * 技術的負債: catchErrorあり
   */
  get<T>(path: string, params?: { [key: string]: string | number | boolean }): Observable<T> {
    let httpParams = new HttpParams();
    if (params) {
      Object.keys(params).forEach(key => {
        if (params[key] != null) {
          httpParams = httpParams.set(key, String(params[key]));
        }
      });
    }

    return this.http.get<T>(`${this.baseUrl}${path}`, { params: httpParams }).pipe(
      catchError(error => {
        console.error(`GET ${path} エラー:`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * POSTリクエスト
   * 技術的負債: catchErrorなし（他メソッドと不一致）
   */
  post<T>(path: string, body: any): Observable<T> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<T>(`${this.baseUrl}${path}`, body, { headers }).pipe(
      catchError(error => {
        console.error(`POST ${path} エラー:`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * PUTリクエスト
   * 技術的負債: catchErrorあり
   */
  put<T>(path: string, body: any): Observable<T> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.put<T>(`${this.baseUrl}${path}`, body, { headers }).pipe(
      catchError(error => {
        console.error(`PUT ${path} エラー:`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * DELETEリクエスト
   * 技術的負債: catchErrorなし（他メソッドと不一致）
   */
  delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${path}`).pipe(
      catchError(error => {
        console.error(`DELETE ${path} エラー:`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * PATCHリクエスト
   */
  patch<T>(path: string, body: any): Observable<T> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.patch<T>(`${this.baseUrl}${path}`, body, { headers }).pipe(
      catchError(error => {
        console.error(`PATCH ${path} エラー:`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * ファイルアップロード（multipart/form-data）
   */
  upload<T>(path: string, file: File, additionalData?: { [key: string]: string }): Observable<T> {
    const formData = new FormData();
    formData.append('file', file);
    if (additionalData) {
      Object.keys(additionalData).forEach(key => {
        formData.append(key, additionalData[key]);
      });
    }
    return this.http.post<T>(`${this.baseUrl}${path}`, formData);
  }

  /**
   * ファイルダウンロード
   */
  download(path: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}${path}`, {
      responseType: 'blob'
    });
  }
}
