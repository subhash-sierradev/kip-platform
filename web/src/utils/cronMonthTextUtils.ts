function formatListWithAnd(items: string[]): string {
  if (items.length === 0) {
    return '';
  }
  if (items.length === 1) {
    return items[0];
  }
  if (items.length === 2) {
    return `${items[0]} and ${items[1]}`;
  }
  return `${items.slice(0, -1).join(', ')} and ${items[items.length - 1]}`;
}

const MONTH_NAMES = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

function parseMonthToken(token: string): string {
  const normalized = token.trim().toUpperCase();
  if (!normalized) {
    return '';
  }
  if (/^\d+$/.test(normalized)) {
    const monthNumber = parseInt(normalized, 10);
    if (monthNumber >= 1 && monthNumber <= 12) {
      return MONTH_NAMES[monthNumber - 1];
    }
    return token.trim();
  }

  const shortMonthIndex = [
    'JAN',
    'FEB',
    'MAR',
    'APR',
    'MAY',
    'JUN',
    'JUL',
    'AUG',
    'SEP',
    'OCT',
    'NOV',
    'DEC',
  ].indexOf(normalized);
  if (shortMonthIndex >= 0) {
    return MONTH_NAMES[shortMonthIndex];
  }

  const fullMonthIndex = MONTH_NAMES.map(name => name.toUpperCase()).indexOf(normalized);
  if (fullMonthIndex >= 0) {
    return MONTH_NAMES[fullMonthIndex];
  }

  return token.trim();
}

function parseMonthValues(mon: string): string[] {
  return mon.split(',').map(parseMonthToken).filter(Boolean);
}

export function monthlyDayText(dom: string, mon: string, formattedTime: string): string {
  const normalizedMonth = mon.trim();
  const dayText = dom.includes(',') ? `days ${dom}` : `day ${dom}`;
  if (normalizedMonth === '*' || !normalizedMonth) {
    return `Runs on ${dayText} of every month at ${formattedTime}`;
  }

  const months = parseMonthValues(normalizedMonth);
  if (months.length === 1 && !dom.includes(',')) {
    return `Runs every year on ${months[0]} ${dom} at ${formattedTime}`;
  }

  const monthText = months.length > 0 ? formatListWithAnd(months) : mon;
  return `Runs on ${dayText} of ${monthText} at ${formattedTime}`;
}

export function monthlyEveryDayText(mon: string, formattedTime: string): string {
  const normalizedMonth = mon.trim();
  if (normalizedMonth === '*' || !normalizedMonth) {
    return `Runs daily at ${formattedTime}`;
  }

  const months = parseMonthValues(normalizedMonth);
  const monthText = months.length > 0 ? formatListWithAnd(months) : mon;
  return `Runs every day in ${monthText} at ${formattedTime}`;
}
