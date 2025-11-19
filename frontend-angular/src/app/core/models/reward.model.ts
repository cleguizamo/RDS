export interface RewardProduct {
  id: number;
  name: string;
  description?: string;
  imageUrl?: string;
  pointsRequired: number;
  stock: number;
  isActive: boolean;
}

export interface RewardProductRequest {
  name: string;
  description?: string;
  imageUrl?: string;
  pointsRequired: number;
  stock: number;
  isActive: boolean;
}

export interface RewardProductResponse {
  id: number;
  name: string;
  description?: string;
  imageUrl?: string;
  pointsRequired: number;
  stock: number;
  isActive: boolean;
}

export interface RedeemRewardRequest {
  rewardProductId: number;
}

