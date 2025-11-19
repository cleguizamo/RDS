export interface Category {
  id: number;
  name: string;
  description?: string;
  productCount?: number;
}

export interface CategoryRequest {
  name: string;
  description?: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description?: string;
  productCount?: number;
}

