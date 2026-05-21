/**
 * サプライヤーモデル定義
 */

/** サプライヤー */
export interface Supplier {
  id: number;
  code: string;
  name: string;
  nameKana: string;
  status: string;
  rating: number;
  address: string;
  phone: string;
  email: string;
  website: string;
  paymentTerms: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
}

/** サプライヤー連絡先 */
export interface SupplierContact {
  id: number;
  supplierId: number;
  name: string;
  department: string;
  position: string;
  phone: string;
  email: string;
  isPrimary: boolean;
}

/** サプライヤー契約 */
export interface SupplierContract {
  id: number;
  supplierId: number;
  contractNumber: string;
  startDate: string;
  endDate: string;
  status: string;
  terms: string;
  totalAmount: number;
  currency: string;
}
