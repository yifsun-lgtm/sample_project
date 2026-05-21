import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private readonly basePath = '/departments';

  constructor(private api: ApiService) {}

  getDepartments(): Observable<any[]> {
    return this.api.get<any[]>(this.basePath);
  }

  getDepartment(id: number): Observable<any> {
    return this.api.get<any>(`${this.basePath}/${id}`);
  }

  createDepartment(data: any): Observable<any> {
    return this.api.post<any>(this.basePath, data);
  }

  updateDepartment(id: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/${id}`, data);
  }

  deleteDepartment(id: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${id}`);
  }
}
