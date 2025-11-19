import { DocumentType } from './user.model';

export { DocumentType };

export interface Employee {
  id: number;
  name: string;
  lastName: string;
  documentType: DocumentType;
  documentNumber: string;
  email: string;
  phone: number;
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

