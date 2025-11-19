export interface SubCategory {
  id: number;
  name: string;
  description?: string;
  categoryId: number;
  categoryName?: string;
  productCount?: number;
}

export interface SubCategoryRequest {
  name: string;
  description?: string;
  categoryId: number;
}

export interface SubCategoryResponse {
  id: number;
  name: string;
  description?: string;
  categoryId: number;
  categoryName?: string;
  productCount?: number;
}

