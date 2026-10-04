import axios from "axios";
import type {
  AnalysisResult,
  GroupSummary,
  OrderItem,
  ShoppingGroup,
} from "./types";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api",
  headers: { "Content-Type": "application/json" },
  timeout: 100_000,
});

export const buyTogetherApi = {
  listGroups: async () => (await api.get<GroupSummary[]>("/groups")).data,
  createGroup: async (name: string) =>
    (await api.post<GroupSummary>("/groups", { name })).data,
  getGroup: async (id: string) => (await api.get<ShoppingGroup>(`/groups/${id}`)).data,
  analyze: async (id: string, conversation: string) =>
    (await api.post<AnalysisResult>(`/groups/${id}/analyze`, { conversation })).data,
  addItem: async (groupId: string, name: string, totalQuantity: number, unit: string) =>
    (await api.post<OrderItem>(`/groups/${groupId}/items`, { name, totalQuantity, unit })).data,
  updateItem: async (item: OrderItem) =>
    (await api.patch<OrderItem>(`/items/${item.id}`, {
      name: item.name,
      totalQuantity: item.totalQuantity,
      unit: item.unit,
    })).data,
  updateStatus: async (id: string, status: OrderItem["status"]) =>
    (await api.patch<OrderItem>(`/items/${id}/status`, { status })).data,
  deleteItem: async (id: string) => api.delete(`/items/${id}`),
};

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? error.message;
  }
  return error instanceof Error ? error.message : "Something went wrong.";
}
