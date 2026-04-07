import type { JiraWebhookDetail } from '@/api/services/JiraWebhookService';
import type { JiraWebhook } from '@/types/JiraWebhook';

export type JiraWebhookData = JiraWebhook | JiraWebhookDetail;
