import axios from 'axios';
import type { DeadLetterItem, EventDetail, EventItem, MetricsSummary, Source } from './contracts';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'
});

type PagedResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
};

export async function getSources(): Promise<Source[]> {
  const response = await api.get<Source[]>('/sources');
  return response.data;
}

export async function getEvents(params?: Record<string, string | number | undefined>): Promise<PagedResponse<EventItem>> {
  const response = await api.get<PagedResponse<EventItem>>('/events', { params });
  return response.data;
}

export async function getEvent(id: string): Promise<EventDetail> {
  const response = await api.get<EventDetail>(`/events/${id}`);
  return response.data;
}

export async function getMetricsSummary(): Promise<MetricsSummary> {
  const response = await api.get<MetricsSummary>('/metrics/summary');
  return response.data;
}

export async function getMetricsBySource(): Promise<Array<Record<string, unknown>>> {
  const response = await api.get<Array<Record<string, unknown>>>('/metrics/by-source');
  return response.data;
}

export async function getMetricsByType(): Promise<Array<Record<string, unknown>>> {
  const response = await api.get<Array<Record<string, unknown>>>('/metrics/by-type');
  return response.data;
}

export async function getDeadLetters(): Promise<DeadLetterItem[]> {
  const response = await api.get<DeadLetterItem[]>('/dead-letters');
  return response.data;
}
