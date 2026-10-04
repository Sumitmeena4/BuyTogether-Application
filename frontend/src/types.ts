export type ShoppingRequest = {
  id: string;
  person: string;
  originalText: string;
  item: string;
  normalizedItem: string;
  quantity: number | null;
  unit: string;
  confidence: number;
  ambiguous: boolean;
  ambiguityReason: string | null;
};

export type OrderItem = {
  id: string;
  name: string;
  totalQuantity: number;
  unit: string;
  status: "OPEN" | "COMPLETED";
  requests: ShoppingRequest[];
};

export type ShoppingGroup = {
  id: string;
  name: string;
  createdAt: string;
  items: OrderItem[];
  requests: ShoppingRequest[];
};

export type GroupSummary = {
  id: string;
  name: string;
  createdAt: string;
  itemCount: number;
};

export type AnalysisResult = {
  group: ShoppingGroup;
  requestCount: number;
  productCount: number;
  ambiguousRequests: ShoppingRequest[];
};
