export interface CacheStat {
  size: number;
  hitRate: number;
  missRate: number;
  requestCount: number;
}

export interface AggregatedStats {
  cacheCount: number;
  totalSize: number;
  requestCount: number;
  hitRate: number;
  missRate: number;
  avgHitRate: number;
  avgMissRate: number;
}
