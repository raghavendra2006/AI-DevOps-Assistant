import { apiClient } from './client';
import {
  IncidentDetail,
  IncidentSummary,
  IncidentAnalyzeRequest,
} from '../types/incident';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const incidentApi = {
  analyzeLog: async (payload: IncidentAnalyzeRequest): Promise<IncidentDetail> => {
    const res = await apiClient.post<IncidentDetail>('/incidents/analyze', payload);
    return res.data;
  },

  analyzeFile: async (formData: FormData): Promise<IncidentDetail> => {
    const res = await apiClient.post<IncidentDetail>('/incidents/analyze/file', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  getIncidentById: async (id: number): Promise<IncidentDetail> => {
    const res = await apiClient.get<IncidentDetail>(`/incidents/${id}`);
    return res.data;
  },

  getRecentIncidents: async (): Promise<IncidentSummary[]> => {
    const res = await apiClient.get<IncidentSummary[]>('/incidents/recent');
    return res.data;
  },

  getUserIncidents: async (page = 0, size = 10): Promise<PageResponse<IncidentSummary>> => {
    const res = await apiClient.get<PageResponse<IncidentSummary>>(`/incidents?page=${page}&size=${size}`);
    return res.data;
  },
};
