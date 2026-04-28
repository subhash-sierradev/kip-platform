import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

// Mock guards to capture required role arrays passed at route creation time
const roleGuardMock = vi.fn().mockImplementation((_roles: string[]) => {
  return vi.fn((_to, _from, next) => next());
});

const allRolesGuardMock = vi.fn().mockImplementation((_roles: string[]) => {
  return vi.fn((_to, _from, next) => next());
});

vi.mock('@/router/guards', () => ({
  roleGuard: roleGuardMock,
  allRolesGuard: allRolesGuardMock,
}));

describe('router/index', () => {
  beforeEach(() => {
    roleGuardMock.mockClear();
    allRolesGuardMock.mockClear();
  });

  afterEach(async () => {
    await vi.resetModules();
  });

  it('defines expected routes and wires role guards', async () => {
    const { default: router } = await import('@/router');

    const paths = router.getRoutes().map(r => r.path);

    // a few key routes should exist
    expect(paths).toContain('/');
    expect(paths).toContain('/inbound/integrations');
    expect(paths).toContain('/outbound/webhook/jira');
    expect(paths).toContain('/outbound/webhook/jira/:id');
    expect(paths).toContain('/outbound/integration/arcgis');
    expect(paths).toContain('/admin/audit-log');
    expect(paths).toContain('/admin/clear-cache');
    expect(paths).toContain('/unauthorized');

    // verify guards were registered with correct role sets
    const calls = roleGuardMock.mock.calls.map(args => args[0] as string[]);

    // jira webhook
    expect(calls).toContainEqual(['feature_jira_webhook']);
    // arcgis integration
    expect(calls).toContainEqual(['feature_arcgis_integration']);
    // admin pages
    expect(calls).toContainEqual(['tenant_admin', 'app_admin']);
    expect(calls).toContainEqual(['app_admin']);
    // confluence connection: must require only tenant_admin — not feature_confluence_integration
    const confluenceGuardCall = roleGuardMock.mock.calls.find(
      ([roles]) => roles.length === 1 && roles[0] === 'tenant_admin'
    );
    expect(confluenceGuardCall).toBeDefined();
    expect(calls).not.toContainEqual(['tenant_admin', 'feature_confluence_integration']);
  });

  it('creates history with configured base url', async () => {
    // Simulate base URL being set
    vi.stubEnv('VITE_BASE_URL', '/myapp');
    const { default: router } = await import('@/router');

    // resolving should include base in href
    const resolved = router.resolve('/');
    expect(resolved.href).toBe('/myapp/');
  });
});
