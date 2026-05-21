import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class DelegationService {
  private readonly basePath = '/delegations';

  constructor(private api: ApiService) {}

  getDelegations(): Observable<any[]> {
    return this.api.get<any[]>(this.basePath);
  }

  getDelegation(id: number): Observable<any> {
    return this.api.get<any>(`${this.basePath}/${id}`);
  }

  createDelegation(data: any): Observable<any> {
    return this.api.post<any>(this.basePath, data);
  }

  updateDelegation(id: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/${id}`, data);
  }

  deleteDelegation(id: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${id}`);
  }

  getUsers(): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/users`);
  }
}
