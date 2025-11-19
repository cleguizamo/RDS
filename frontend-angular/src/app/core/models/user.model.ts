export enum Role {
  CLIENT = 'CLIENT',
  EMPLOYEE = 'EMPLOYEE',
  ADMIN = 'ADMIN'
}

export enum DocumentType {
  CC = 'CC',
  CE = 'CE',
  TI = 'TI'
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  name: string;
  lastName: string;
  role: Role;
  userId: number;
  redirectTo: string;
}

export interface User {
  id: number;
  email: string;
  name: string;
  lastName: string;
  role: Role;
}

export interface UserResponse {
  id: number;
  name: string;
  lastName: string;
  documentType: DocumentType;
  documentNumber: string;
  phone: number;
  email: string;
  points: number;
  dateOfBirth: string;
  numberOfOrders: number;
  totalSpent: number;
  lastOrderDate?: string;
  numberOfReservations: number;
}

export interface UserUpdateRequest {
  name: string;
  lastName: string;
  documentType: DocumentType;
  documentNumber: string;
  phone: number;
  email: string;
  dateOfBirth: string;
  points?: number;
  totalSpent?: number;
}

export interface SignUpRequest {
  name: string;
  lastName: string;
  documentType: DocumentType;
  documentNumber: string;
  phone: string;
  email: string;
  password: string;
  dateOfBirth: string;
}

