const LOCALE = 'zh-CN';

const dateTimeFormatter = new Intl.DateTimeFormat(LOCALE, {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

const timeFormatter = new Intl.DateTimeFormat(LOCALE, {
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

const numberFormatter = new Intl.NumberFormat(LOCALE);

const getDateTimeParts = (date: Date): Record<string, string> => {
  const parts: Record<string, string> = {};
  for (const part of dateTimeFormatter.formatToParts(date)) {
    if (part.type !== 'literal') {
      parts[part.type] = part.value;
    }
  }
  return parts;
};

export const formatDateTime = (value: string | undefined | null): string => {
  if (!value) return '—';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  const parts = getDateTimeParts(date);
  return `${parts.year ?? '0000'}-${parts.month ?? '00'}-${parts.day ?? '00'} ${parts.hour ?? '00'}:${parts.minute ?? '00'}`;
};

export const formatRelativeDateTime = (value: string | undefined | null): string => {
  if (!value) return '—';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const startOfYesterday = new Date(startOfToday);
  startOfYesterday.setDate(startOfYesterday.getDate() - 1);

  const time = timeFormatter.format(date);
  if (date >= startOfToday) return `今天 ${time}`;
  if (date >= startOfYesterday) return `昨天 ${time}`;
  return formatDateTime(value);
};

export const formatNumber = (value: number): string => numberFormatter.format(value);
