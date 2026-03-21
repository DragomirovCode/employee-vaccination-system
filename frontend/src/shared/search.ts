export function normalizeSearchValue(value: string | number | null | undefined): string {
  if (value === null || value === undefined) {
    return "";
  }

  return String(value).trim().toLocaleLowerCase();
}

export function matchesSearchQuery(
  query: string,
  ...values: Array<string | number | null | undefined>
): boolean {
  const normalizedQuery = normalizeSearchValue(query);
  if (!normalizedQuery) {
    return true;
  }

  return values.some((value) => normalizeSearchValue(value).includes(normalizedQuery));
}

export function getDateSearchValues(
  value: string | null | undefined,
  locale: string,
  options?: Intl.DateTimeFormatOptions
): string[] {
  if (!value) {
    return [];
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return [value];
  }

  return [value, new Intl.DateTimeFormat(locale, options).format(date)];
}
