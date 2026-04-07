import fs from 'node:fs';
import path from 'node:path';

import { describe, expect, it } from 'vitest';

describe('layout.css', () => {
  it('exists and contains key selectors and variables', () => {
    const cssPath = path.resolve(process.cwd(), 'src', 'components', 'layout', 'layout.css');
    expect(fs.existsSync(cssPath)).toBe(true);
    const content = fs.readFileSync(cssPath, 'utf-8');
    expect(content).toContain(':root');
    expect(content).toContain('--kw-accent-orange');
    expect(content).toContain('.modern-topbar');
    expect(content).toContain('.kaseware-sidebar');
  });
});
