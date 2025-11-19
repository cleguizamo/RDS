export interface Product {
  id?: number;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  categoryId: number;
  categoryName: string;
  subCategoryId?: number;
  subCategoryName?: string;
  stock: number;
}

export interface ProductRequest {
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  categoryId: number;
  subCategoryId?: number;
  stock: number;
}

export interface ProductResponse {
  id: number;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  categoryId: number;
  categoryName: string;
  subCategoryId?: number;
  subCategoryName?: string;
  stock: number;
}

