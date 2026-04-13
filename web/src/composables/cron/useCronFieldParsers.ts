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

function addSteppedNumericValues(
  start: number,
  end: number,
  step: number,
  min: number,
  max: number,
  numbers: number[]
): void {
  if (Number.isNaN(start) || Number.isNaN(end) || Number.isNaN(step) || step <= 0) {
    return;
  }

  addForwardSteppedValues(start, end, step, min, max, numbers);
  if (start <= end) {
    return;
  }
  addForwardSteppedValues(start, max, step, min, max, numbers);
  addForwardSteppedValues(min, end, step, min, max, numbers);
}

function addForwardSteppedValues(
  start: number,
  end: number,
  step: number,
  min: number,
  max: number,
  numbers: number[]
): void {
  for (let value = start; value <= end; value += step) {
    if (value >= min && value <= max && !numbers.includes(value)) {
      numbers.push(value);
    }
  }
}

const MONTH_TOKEN_MAP: Record<string, number> = {
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

function resolveMonthToken(token: string): number {
  const upper = token.toUpperCase();
  const mapped = MONTH_TOKEN_MAP[upper];
  return mapped ?? parseInt(upper, 10);
}

function addMonthStepToken(token: string, numbers: number[]): boolean {
  if (!token.includes('/')) {
    return false;
  }

  const [baseToken, stepToken] = token.split('/').map(part => part.trim());
  const step = parseInt(stepToken, 10);
  if (Number.isNaN(step) || step <= 0) {
    return true;
  }

  if (baseToken === '*') {
    addSteppedNumericValues(1, 12, step, 1, 12, numbers);
    return true;
  }

  if (baseToken.includes('-')) {
    const [startToken, endToken] = baseToken.split('-').map(part => part.trim());
    addSteppedNumericValues(
      resolveMonthToken(startToken),
      resolveMonthToken(endToken),
      step,
      1,
      12,
      numbers
    );
    return true;
  }

  addSteppedNumericValues(resolveMonthToken(baseToken), 12, step, 1, 12, numbers);
  return true;
}

function addMonthRangeToken(token: string, numbers: number[]): boolean {
  if (!token.includes('-')) {
    return false;
  }

  const [startToken, endToken] = token.split('-').map(part => part.trim());
  addSteppedNumericValues(
    resolveMonthToken(startToken),
    resolveMonthToken(endToken),
    1,
    1,
    12,
    numbers
  );
  return true;
}

function addMonthSingleToken(token: string, numbers: number[]): void {
  const value = resolveMonthToken(token);
  if (!Number.isNaN(value) && value >= 1 && value <= 12 && !numbers.includes(value)) {
    numbers.push(value);
  }
}

export function parseMonthField(field: string): number[] {
  if (field === '*') return Array.from({ length: 12 }, (_, i) => i + 1);
  const numbers: number[] = [];
  for (const token of field.split(',').map(t => t.trim().toUpperCase())) {
    if (!addMonthStepToken(token, numbers) && !addMonthRangeToken(token, numbers)) {
      addMonthSingleToken(token, numbers);
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

function getLastUtcDayOfMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
}

function getNearestWeekdayDate(
  targetDate: number,
  year: number,
  month: number,
  lastDay: number
): number {
  const utcDow = new Date(Date.UTC(year, month, targetDate)).getUTCDay();
  if (utcDow !== 0 && utcDow !== 6) {
    return targetDate;
  }
  if (utcDow === 6) {
    return targetDate === 1 ? 3 : targetDate - 1;
  }
  return targetDate === lastDay ? targetDate - 2 : targetDate + 1;
}

export function resolveNearestWeekdayDate(
  dayField: string,
  year: number,
  month: number
): number | null {
  const upper = dayField.toUpperCase();
  const lastDay = getLastUtcDayOfMonth(year, month);

  if (upper === 'LW') {
    return getNearestWeekdayDate(lastDay, year, month, lastDay);
  }

  const lastOffsetMatch = upper.match(/^L-(\d+)W$/);
  if (lastOffsetMatch) {
    const offset = parseInt(lastOffsetMatch[1], 10);
    const targetDate = lastDay - offset;
    if (targetDate < 1) {
      return null;
    }
    return getNearestWeekdayDate(targetDate, year, month, lastDay);
  }

  const nearestWeekdayMatch = upper.match(/^(\d{1,2})W$/);
  if (!nearestWeekdayMatch) {
    return null;
  }

  const rawTargetDate = parseInt(nearestWeekdayMatch[1], 10);
  if (Number.isNaN(rawTargetDate) || rawTargetDate < 1 || rawTargetDate > 31) {
    return null;
  }

  const targetDate = Math.min(rawTargetDate, lastDay);
  return getNearestWeekdayDate(targetDate, year, month, lastDay);
}

function addResolvedDayMatch(candidate: number | null, matches: number[]): boolean {
  if (candidate === null) {
    return false;
  }
  if (!matches.includes(candidate)) {
    matches.push(candidate);
  }
  return true;
}

function addLastDayTokenMatch(
  token: string,
  year: number,
  month: number,
  matches: number[]
): boolean {
  const upper = token.toUpperCase();
  const lastDay = getLastUtcDayOfMonth(year, month);
  if (upper === 'L') {
    return addResolvedDayMatch(lastDay, matches);
  }

  const lastOffsetMatch = upper.match(/^L-(\d+)$/);
  if (!lastOffsetMatch) {
    return false;
  }

  const candidate = lastDay - parseInt(lastOffsetMatch[1], 10);
  return addResolvedDayMatch(candidate >= 1 ? candidate : null, matches);
}

function addDayOfMonthStepToken(
  token: string,
  year: number,
  month: number,
  matches: number[]
): boolean {
  const upper = token.toUpperCase();
  if (!upper.includes('/')) {
    return false;
  }

  const lastDay = getLastUtcDayOfMonth(year, month);
  const [baseToken, stepToken] = upper.split('/').map(part => part.trim());
  const step = parseInt(stepToken, 10);
  if (Number.isNaN(step) || step <= 0) {
    return true;
  }

  if (baseToken === '*') {
    addSteppedNumericValues(1, lastDay, step, 1, lastDay, matches);
    return true;
  }

  if (baseToken.includes('-')) {
    const [startToken, endToken] = baseToken.split('-').map(part => parseInt(part.trim(), 10));
    addSteppedNumericValues(startToken, Math.min(endToken, lastDay), step, 1, lastDay, matches);
    return true;
  }

  addSteppedNumericValues(parseInt(baseToken, 10), lastDay, step, 1, lastDay, matches);
  return true;
}

function addDayOfMonthRangeToken(
  token: string,
  year: number,
  month: number,
  matches: number[]
): boolean {
  const upper = token.toUpperCase();
  if (!upper.includes('-')) {
    return false;
  }

  const lastDay = getLastUtcDayOfMonth(year, month);
  const [startToken, endToken] = upper.split('-').map(part => parseInt(part.trim(), 10));
  addSteppedNumericValues(startToken, Math.min(endToken, lastDay), 1, 1, lastDay, matches);
  return true;
}

function addDayOfMonthSingleToken(
  token: string,
  year: number,
  month: number,
  matches: number[]
): void {
  const upper = token.toUpperCase();
  const lastDay = getLastUtcDayOfMonth(year, month);
  const value = parseInt(upper, 10);
  if (!Number.isNaN(value) && value >= 1 && value <= lastDay && !matches.includes(value)) {
    matches.push(value);
  }
}

function addDayOfMonthTokenMatches(
  token: string,
  year: number,
  month: number,
  matches: number[]
): void {
  if (addResolvedDayMatch(resolveNearestWeekdayDate(token, year, month), matches)) {
    return;
  }

  if (addLastDayTokenMatch(token, year, month, matches)) {
    return;
  }

  if (addDayOfMonthStepToken(token, year, month, matches)) {
    return;
  }

  if (addDayOfMonthRangeToken(token, year, month, matches)) {
    return;
  }

  addDayOfMonthSingleToken(token, year, month, matches);
}

export function collectDayOfMonthMatches(field: string, year: number, month: number): number[] {
  if (field === '*' || field === '?') {
    const lastDay = getLastUtcDayOfMonth(year, month);
    return Array.from({ length: lastDay }, (_, index) => index + 1);
  }

  const matches: number[] = [];
  for (const token of field.split(',').map(part => part.trim())) {
    addDayOfMonthTokenMatches(token, year, month, matches);
  }

  return matches.sort((a, b) => a - b);
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

function resolveQuartzDowOrdinal(token: string): number {
  const upper = token.toUpperCase();
  const dayMap: Record<string, number> = {
    SUN: 1,
    MON: 2,
    TUE: 3,
    WED: 4,
    THU: 5,
    FRI: 6,
    SAT: 7,
  };
  const mapped = dayMap[upper];
  if (mapped !== undefined) {
    return mapped;
  }
  const parsed = parseInt(token, 10);
  if (Number.isNaN(parsed)) {
    return -1;
  }
  return parsed === 0 ? 1 : parsed;
}

function addQuartzDowValues(start: number, end: number, step: number, numbers: number[]): void {
  const ordinals: number[] = [];
  addSteppedNumericValues(start, end, step, 1, 7, ordinals);
  for (const ordinal of ordinals) {
    const utcIndex = ordinal === 1 ? 0 : ordinal - 1;
    if (!numbers.includes(utcIndex)) {
      numbers.push(utcIndex);
    }
  }
}

function addQuartzDowStepToken(token: string, numbers: number[]): boolean {
  if (!token.includes('/')) {
    return false;
  }

  const [baseToken, stepToken] = token.split('/').map(part => part.trim());
  const step = parseInt(stepToken, 10);
  if (Number.isNaN(step) || step <= 0) {
    return true;
  }

  if (baseToken === '*') {
    addQuartzDowValues(1, 7, step, numbers);
    return true;
  }

  if (baseToken.includes('-')) {
    const [startToken, endToken] = baseToken.split('-').map(part => part.trim());
    addQuartzDowValues(
      resolveQuartzDowOrdinal(startToken),
      resolveQuartzDowOrdinal(endToken),
      step,
      numbers
    );
    return true;
  }

  addQuartzDowValues(resolveQuartzDowOrdinal(baseToken), 7, step, numbers);
  return true;
}

function addQuartzDowRangeToken(token: string, numbers: number[]): boolean {
  if (!token.includes('-')) {
    return false;
  }

  const [startToken, endToken] = token.split('-').map(part => part.trim());
  addQuartzDowValues(
    resolveQuartzDowOrdinal(startToken),
    resolveQuartzDowOrdinal(endToken),
    1,
    numbers
  );
  return true;
}

function addQuartzDowSingleToken(token: string, numbers: number[]): void {
  const value = quartzDowToUtcIndex(token);
  if (!Number.isNaN(value) && value >= 0 && value <= 6 && !numbers.includes(value)) {
    numbers.push(value);
  }
}

export function parseDayOfWeekField(field: string): number[] {
  if (field === '?' || field === '*') {
    return Array.from({ length: 7 }, (_, i) => i);
  }
  const numbers: number[] = [];
  for (const token of field.split(',').map(t => t.trim().toUpperCase())) {
    if (!addQuartzDowStepToken(token, numbers) && !addQuartzDowRangeToken(token, numbers)) {
      addQuartzDowSingleToken(token, numbers);
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
