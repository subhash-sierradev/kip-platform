import type { IntegrationScheduleResponse } from '@/api/models/IntegrationScheduleResponse';

export interface HasSchedule {
  schedule?: IntegrationScheduleResponse | null;
  nextRunAtUtc?: string | null;
}
