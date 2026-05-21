import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { UserProfile, Role } from '@shared/models/user.model';
import { PageResult } from '@shared/models/common.model';

/**
 * ユーザーサービス
 * ユーザー管理のAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class UserService {

  private readonly basePath = '/users';

  constructor(private api: ApiService) {}

  /** ユーザー一覧を取得 */
  getUsers(page: number = 0, size: number = 20): Observable<PageResult<UserProfile>> {
    return this.api.get<PageResult<UserProfile>>(this.basePath, { page, size });
  }

  /** ユーザー詳細を取得 */
  getUser(id: string): Observable<UserProfile> {
    return this.api.get<UserProfile>(`${this.basePath}/${id}`);
  }

  /** 現在のユーザー情報を取得 */
  getCurrentUser(): Observable<UserProfile> {
    return this.api.get<UserProfile>(`${this.basePath}/me`);
  }

  /** ロール一覧を取得 */
  getRoles(): Observable<Role[]> {
    return this.api.get<Role[]>(`${this.basePath}/roles`);
  }

  /** ユーザープロファイルを更新 */
  updateUser(id: number | string, data: any): Observable<UserProfile> {
    return this.api.put<UserProfile>(`${this.basePath}/${id}`, data);
  }

  /** ユーザーにロールを割り当て */
  assignRole(userId: string, roleId: string): Observable<void> {
    return this.api.post<void>(`${this.basePath}/${userId}/roles`, { roleId });
  }
}
