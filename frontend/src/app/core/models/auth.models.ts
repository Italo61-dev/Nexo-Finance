export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface AuthResponse {
  token: string;
  type: string;
}

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp?: string;
  path?: string;
}
