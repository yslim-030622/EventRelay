export type Source = {
  id: number;
  name: string;
  displayName: string;
  active: boolean;
  createdAt: string;
};

export type EventItem = {
  id: number;
  eventId: string;
  source: string;
  eventType: string;
  status: string;
  createdAt: string;
  processedAt: string | null;
};

export type DeliveryAttempt = {
  id: number;
  consumerName: string;
  attemptNumber: number;
  status: string;
  durationMs: number | null;
  errorMessage: string | null;
  createdAt: string;
};

export type EventDetail = EventItem & {
  retryCount: number;
  lastError: string | null;
  headers: Record<string, unknown>;
  payload: Record<string, unknown>;
  deliveries: DeliveryAttempt[];
};

export type MetricsSummary = {
  totalEvents: number;
  receivedEvents: number;
  processingEvents: number;
  processedEvents: number;
  failedEvents: number;
  deadLetterEvents: number;
};

export type DeadLetterItem = {
  id: number;
  eventPk: number;
  eventId: string;
  eventType: string;
  source: string;
  errorMessage: string;
  replayed: boolean;
  createdAt: string;
  replayedAt: string | null;
};

export type CircuitBreakerInfo = {
  name: string;
  state: string;
  failureRate: number;
  slowCallRate: number;
  bufferedCalls: number;
  failedCalls: number;
  successfulCalls: number;
};
