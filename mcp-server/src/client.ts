import axios from 'axios';

const SPRING_BASE_URL = process.env.SPRING_BASE_URL ?? 'http://localhost:8080';

export const springClient = axios.create({
  baseURL: SPRING_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

export async function getQaFailures(teamId: number, token: string) {
  const response = await springClient.get('/api/v1/qa/failures', {
    params: { teamId },
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
}

export async function getTasks(teamId: number, token: string, status?: string) {
  const params: Record<string, unknown> = {};
  if (status) params['status'] = status;
  const response = await springClient.get(`/api/v1/tasks/teams/${teamId}`, {
    params,
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
}

export async function getTaskDetails(taskId: string, token: string) {
  const response = await springClient.get(`/api/v1/tasks/${taskId}/details`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
}

export async function getApiSchema(endpointId: number, token: string) {
  const response = await springClient.get(`/api/v1/endpoints/${endpointId}/schema`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
}
