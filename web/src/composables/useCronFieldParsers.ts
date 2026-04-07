export function parseNumericField(field: string, min: number, max: number): number[] {
  if (field === '*' || field === '?') {
    return Array.from({ length: max - min + 1 }, (_, i) => min + i);
  }
  const numbers: number[] = [];
  for (const token of field.split(',').map(t => t.trim())) {
    addRangeOrValue(token, min, max, numbers);
  }
  return numbers.sort((a, b) => a - b);
}

function addRangeOrValue(token: string, min: number, max: number, numbers: number[]): void {
  if (token.includes('-')) {
    addRangeValues(token, min, max, numbers);
  } else if (token.includes('/')) {
    addStepValues(token, min, max, numbers);
  } else {
    addSingleValue(token, min, max, numbers);
  }
}

function addRangeValues(token: string, min: number, max: number, numbers: number[]): void {
  const [start, end] = token.split('-').map(t => parseInt(t.trim(), 10));
  for (let i = start; i <= end && i <= max; i++) {
    if (i >= min && !numbers.includes(i)) numbers.push(i);
  }
}

function addStepValues(token: string, min: number, max: number, numbers: number[]): void {
  const [startToken, intervalToken] = token.split('/').map(t => t.trim());
  const start = startToken === '*' ? min : parseInt(startToken, 10);
  const step = parseInt(intervalToken, 10);
  if (Number.isNaN(step) || step <= 0) {
    return;
  }
  for (let i = start; i <= max; i += step) {
    if (i >= min && !numbers.includes(i)) numbers.push(i);
  }
}

function addSingleValue(token: string, min: number, max: number, numbers: number[]): void {
  const value = parseInt(token, 10);
  if (!Number.isNaN(value) && value >= min && value <= max && !numbers.includes(value)) {
    numbers.push(value);
  }
}

export function parseMonthField(field: string): number[] {
  const monthMap: Record<string, number> = {
    JAN: 1,
    FEB: 2,
    MAR: 3,
    APR: 4,
    MAY: 5,
    JUN: 6,
    JUL: 7,
    AUG: 8,
    SEP: 9,
    OCT: 10,
    NOV: 11,
    DEC: 12,
  };
  if (field === '*') return Array.from({ length: 12 }, (_, i) => i + 1);
  const numbers: number[] = [];
  for (const token of field.split(',').map(t => t.trim().toUpperCase())) {
    const mapped = monthMap[token];
    const value = mapped ?? parseInt(token, 10);
    if (!Number.isNaN(value) && value >= 1 && value <= 12 && !numbers.includes(value)) {
      numbers.push(value);
    }
  }
  return numbers.sort((a, b) => a - b);
}

export function resolveQuartzDowIndex(token: string): number {
  const dayNameMap: Record<string, number> = {
    SUN: 0,
    MON: 1,
    TUE: 2,
    WED: 3,
    THU: 4,
    FRI: 5,
    SAT: 6,
  };
  const upper = token.toUpperCase();
  if (dayNameMap[upper] !== undefined) return dayNameMap[upper];
  const n = parseInt(token, 10);
  if (Number.isNaN(n)) return -1;
  if (n === 0) return 0; // support 0-as-Sunday, consistent with quartzDowToUtcIndex and validation
  if (n < 0) return -1; // avoid negative modulo for unexpected negative inputs
  return (n - 1) % 7;
}

export function matchesNthWeekday(
  date: number,
  utcDow: number,
  dowToken: string,
  nth: number
): boolean {
  const targetDow = resolveQuartzDowIndex(dowToken);
  if (targetDow === -1 || utcDow !== targetDow) return false;
  return Math.ceil(date / 7) === nth;
}

export function matchesLastWeekday(
  date: number,
  utcDow: number,
  month: number,
  year: number,
  dowToken: string
): boolean {
  const targetDow = resolveQuartzDowIndex(dowToken);
  if (targetDow === -1 || utcDow !== targetDow) return false;
  const lastDayOfMonth = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  return date + 7 > lastDayOfMonth;
}

// Quartz DOW: 1=Sun, 2=Mon, ..., 7=Sat; 0 accepted as Sun (standard compat)
function quartzDowToUtcIndex(token: string): number {
  const dayMap: Record<string, number> = {
    SUN: 0,
    MON: 1,
    TUE: 2,
    WED: 3,
    THU: 4,
    FRI: 5,
    SAT: 6,
  };
  const m = dayMap[token];
  if (m !== undefined) return m;
  const n = parseInt(token, 10);
  return n === 0 ? 0 : (n - 1) % 7;
}

export function parseDayOfWeekField(field: string): number[] {
  if (field === '?' || field === '*') {
    return Array.from({ length: 7 }, (_, i) => i);
  }
  const numbers: number[] = [];
  for (const token of field.split(',').map(t => t.trim().toUpperCase())) {
    if (token.includes('-')) {
      const [s, e] = token.split('-');
      for (let i = quartzDowToUtcIndex(s); i <= quartzDowToUtcIndex(e); i++) {
        if (!numbers.includes(i)) numbers.push(i);
      }
      continue;
    }
    const value = quartzDowToUtcIndex(token);
    if (!Number.isNaN(value) && value >= 0 && value <= 6 && !numbers.includes(value)) {
      numbers.push(value);
    }
  }
  return numbers.sort((a, b) => a - b);
}

export function toOrdinal(value: number): string {
  const mod10 = value % 10;
  const mod100 = value % 100;
  if (mod10 === 1 && mod100 !== 11) return `${value}st`;
  if (mod10 === 2 && mod100 !== 12) return `${value}nd`;
  if (mod10 === 3 && mod100 !== 13) return `${value}rd`;
  return `${value}th`;
}

export function normalizeOffsetLabel(rawOffset: string): string {
  if (rawOffset === 'GMT' || rawOffset === 'UTC') return 'GMT+00:00';
  const match = rawOffset.match(/^GMT([+-])(\d{1,2})(?::(\d{2}))?$/);
  if (!match) return rawOffset;
  const [, sign, hoursRaw, minutesRaw] = match;
  const hours = hoursRaw.padStart(2, '0');
  const minutes = (minutesRaw || '00').padStart(2, '0');
  return `GMT${sign}${hours}:${minutes}`;
}
