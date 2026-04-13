import { describe, expect, it } from 'vitest';

import * as strategies from '@/strategies';

describe('strategies index', () => {
  it('re-exports the public strategy classes', () => {
    expect(strategies.ArcGISAPIKeyStrategy).toBeDefined();
    expect(strategies.ArcGISBasicAuthStrategy).toBeDefined();
    expect(strategies.ArcGISOAuth2Strategy).toBeDefined();
    expect(strategies.BaseCredentialStrategy).toBeDefined();
    expect(strategies.CredentialStrategyRegistry).toBeDefined();
    expect(strategies.JiraBasicAuthStrategy).toBeDefined();
  });
});
