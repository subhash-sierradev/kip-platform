import type { LanguageDto } from '@/api/models/LanguageDto';

import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class MasterDataService {
  public static getAllActiveLanguages(): Promise<LanguageDto[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/api/master-data/languages',
    });
  }
}
