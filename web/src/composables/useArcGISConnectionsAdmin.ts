import { ServiceType } from '@/api/models/enums';

import { useServiceConnectionsAdmin } from './useServiceConnectionsAdmin';

/**
 * ArcGIS-specific connection administration composable
 * Provides connection management functionality for ArcGIS integrations
 */
export function useArcGISConnectionsAdmin() {
  return useServiceConnectionsAdmin(ServiceType.ARCGIS);
}
