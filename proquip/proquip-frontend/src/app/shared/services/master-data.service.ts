import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class MasterDataService {

  private readonly basePath = '/master-data';

  constructor(private api: ApiService) {}

  getManufacturers(): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/manufacturers`);
  }

  createManufacturer(data: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/manufacturers`, data);
  }

  updateManufacturer(id: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/manufacturers/${id}`, data);
  }

  deleteManufacturer(id: number): Observable<any> {
    return this.api.delete<any>(`${this.basePath}/manufacturers/${id}`);
  }

  getUnits(): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/units`);
  }

  createUnit(data: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/units`, data);
  }

  updateUnit(id: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/units/${id}`, data);
  }

  deleteUnit(id: number): Observable<any> {
    return this.api.delete<any>(`${this.basePath}/units/${id}`);
  }

  getCurrencies(): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/currencies`);
  }

  createCurrency(data: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/currencies`, data);
  }

  updateCurrency(id: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/currencies/${id}`, data);
  }

  getTaxRates(): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/tax-rates`);
  }

  createTaxRate(data: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/tax-rates`, data);
  }

  updateTaxRate(id: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/tax-rates/${id}`, data);
  }
}
