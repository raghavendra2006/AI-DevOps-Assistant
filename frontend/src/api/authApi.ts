import { apiClient } from './client';
import {
  AuthResponse,
  ApiResponse,
  LoginPayload,
  RegisterPayload,
  ForgotPasswordPayload,
  ResetPasswordPayload,
  User,
} from '../types/auth';

export const authApi = {
  register: async (payload: RegisterPayload): Promise<ApiResponse<string>> => {
    const res = await apiClient.post<ApiResponse<string>>('/auth/register', payload);
    return res.data;
  },

  verifyEmail: async (token: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.get<ApiResponse<string>>(`/auth/verify-email?token=${token}`);
    return res.data;
  },

  login: async (payload: LoginPayload): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/login', payload);
    return res.data;
  },

  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/refresh', { refreshToken });
    return res.data;
  },

  logout: async (): Promise<ApiResponse<void>> => {
    const res = await apiClient.post<ApiResponse<void>>('/auth/logout');
    return res.data;
  },

  forgotPassword: async (payload: ForgotPasswordPayload): Promise<ApiResponse<string>> => {
    const res = await apiClient.post<ApiResponse<string>>('/auth/forgot-password', payload);
    return res.data;
  },

  resetPassword: async (payload: ResetPasswordPayload): Promise<ApiResponse<void>> => {
    const res = await apiClient.post<ApiResponse<void>>('/auth/reset-password', payload);
    return res.data;
  },

  getMe: async (): Promise<User> => {
    const res = await apiClient.get<User>('/auth/me');
    return res.data;
  },

  getUsers: async (page = 0, size = 20) => {
    const res = await apiClient.get(`/users?page=${page}&size=${size}`);
    return res.data;
  },

  updateUserRoles: async (userId: number, roles: string[]): Promise<User> => {
    const res = await apiClient.put<User>(`/users/${userId}/roles`, { roles });
    return res.data;
  },

  getRecentAuditLogs: async () => {
    const res = await apiClient.get('/audit-logs/recent');
    return res.data;
  },
};
