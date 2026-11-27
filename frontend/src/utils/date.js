export function toMonthKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

export function toDateKey(year, monthIndex, day) {
  return `${year}-${String(monthIndex + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

export function parseMonthKey(monthKey) {
  const [year, month] = monthKey.split('-').map(Number);
  return { year, monthIndex: month - 1 };
}

export function shiftMonth(monthKey, amount) {
  const { year, monthIndex } = parseMonthKey(monthKey);
  return toMonthKey(new Date(year, monthIndex + amount, 1));
}

export function getMonthDays(monthKey) {
  const { year, monthIndex } = parseMonthKey(monthKey);
  const daysCount = new Date(year, monthIndex + 1, 0).getDate();
  return Array.from({ length: daysCount }, (_, index) => {
    const day = index + 1;
    return {
      day,
      dateKey: toDateKey(year, monthIndex, day),
      weekday: new Date(year, monthIndex, day).getDay(),
    };
  });
}

export function getCalendarCells(monthKey) {
  const { year, monthIndex } = parseMonthKey(monthKey);
  const emptySlots = new Date(year, monthIndex, 1).getDay();
  return [...Array(emptySlots).fill(null), ...getMonthDays(monthKey)];
}

export function formatVietnameseDate(date) {
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh',
  }).format(date);
}
