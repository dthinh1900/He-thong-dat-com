export const money = (value) => new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 }).format(Number(value ?? 0)) + 'đ';
export const dateLabel = (value) => value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—';

export function sortVietnamese(items, key) {
  return [...items].sort((a, b) => String(a[key] ?? '').localeCompare(String(b[key] ?? ''), 'vi'));
}
