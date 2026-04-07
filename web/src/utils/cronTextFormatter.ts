import type { ArcGISIntegrationSummaryResponse } from '@/api/models/ArcGISIntegrationSummaryResponse';
import { cronToText } from '@/composables/useCron';

interface SchedulableIntegration {
  frequencyPattern?: string;
  cronExpression?: string;
}

/**
 * Converts a cron expression to human-readable text.
 * Used for displaying CUSTOM schedule patterns.
 */
function getScheduleTextFromCronExpression(cronExpression: string | undefined): string {
  if (!cronExpression) {
    return 'Custom schedule';
  }
  const text = cronToText(cronExpression);
  return text || 'Custom schedule';
}

/**
 * Gets formatted schedule text for CUSTOM cron pattern integrations.
 * Returns a human-readable description of the cron schedule.
 */
export function getScheduleTextForCustomPattern(
  scheduleData: SchedulableIntegration | ArcGISIntegrationSummaryResponse
): string {
  const data = scheduleData as { cronExpression?: string };
  return getScheduleTextFromCronExpression(data.cronExpression);
}
