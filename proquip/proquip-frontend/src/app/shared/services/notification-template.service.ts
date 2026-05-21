import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class NotificationTemplateService {
  private readonly basePath = '/notification-templates';

  constructor(private api: ApiService) {}

  getTemplates(): Observable<any[]> {
    return this.api.get<any[]>(this.basePath);
  }

  getTemplate(id: number): Observable<any> {
    return this.api.get<any>(`${this.basePath}/${id}`);
  }

  updateTemplate(id: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/${id}`, data);
  }

  toggleActive(id: number): Observable<any> {
    return this.api.put<any>(`${this.basePath}/${id}/toggle`, {});
  }
}
