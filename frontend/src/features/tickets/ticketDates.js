import { todayKey, dateShift, isWorkday } from '../../utils/mealDates';

export function nextWorkday() {
  let date = dateShift(todayKey(), 1);
  while (!isWorkday(date)) date = dateShift(date, 1);
  return date;
}
