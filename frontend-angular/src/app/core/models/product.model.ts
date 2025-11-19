export interface Product {
  id?: number;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  category: string;
  stock: number;
}

export interface ProductRequest {
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  category: string;
  stock: number;
}

export interface ProductResponse {
  id: number;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  category: string;
  stock: number;
}

