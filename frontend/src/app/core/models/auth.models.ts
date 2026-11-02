export type Role = 'ROLE_USER' | 'ROLE_ADMIN';
export type AuthProviderType = 'LOCAL' | 'GOOGLE';

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
  role?: Role;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserSummary {
  id: number;
  name: string;
  email: string;
  authProvider: AuthProviderType;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface PlatformMetrics {
  totalUsers: number;
  totalAdmins: number;
  googleAuthPercent: number;
  inactiveUsers: number;
}

export interface UpdateUserRoleRequest {
  role: Role;
}

export interface UpdateUserStatusRequest {
  active: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp?: string;
  path?: string;
  validationErrors?: Array<{ field: string; message: string }>;
}
