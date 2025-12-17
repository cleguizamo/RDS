import { DocumentType } from './user.model';

export { DocumentType };

export enum PaymentFrequency {
  MONTHLY = 'MONTHLY',
  BIWEEKLY = 'BIWEEKLY'
}

export interface Employee {
  id: number;
  name: string;
  lastName: string;
  documentType: DocumentType;
  documentNumber: string;
  email: string;
  phone: number;
  salary?: number | null;
  paymentFrequency?: PaymentFrequency | null;
  paymentDay?: number | null;
}

export interface EmployeeRequest {
  name: string;
  lastName: string;
  documentType: DocumentType;
  documentNumber: string;
  email: string;
  password: string;
  phone: string;
}

export interface SalaryUpdateRequest {
  salary: number;
  paymentFrequency: PaymentFrequency;
  paymentDay: number;
}

export interface SalaryPayment {
  id: number;
  employeeId: number;
  employeeName: string;
  employeeLastName: string;
  amount: number;
  paymentDate: string;
  periodStartDate: string;
  periodEndDate: string;
  createdAt: string;
  paymentFrequency: PaymentFrequency;
  status?: PaymentStatus;
  processedAt?: string;
  failureReason?: string;
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

