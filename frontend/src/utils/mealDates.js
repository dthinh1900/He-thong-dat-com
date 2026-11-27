/** Asia/Ho_Chi_Minh date handling. Date strings always YYYY-MM-DD, no UTC/local offset leaks. */
const zone = 'Asia/Ho_Chi_Minh';
export function todayKey(now = new Date()) {
  const p = new Intl.DateTimeFormat('en-CA', { timeZone: zone, year:'numeric', month:'2-digit', day:'2-digit' }).formatToParts(now);
  const m = Object.fromEntries(p.map(x=>[x.type,x.value]));
  return `${m.year}-${m.month}-${m.day}`;
}
function minuteVN(now = new Date()) {
  const p = new Intl.DateTimeFormat('en-GB',{timeZone:zone,hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).formatToParts(now);
  const m=Object.fromEntries(p.map(x=>[x.type,x.value]));return Number(m.hour)*60+Number(m.minute);
}
export function dateShift(key, n) {
  const d = new Date(`${key}T12:00:00Z`); d.setUTCDate(d.getUTCDate()+n);return d.toISOString().slice(0,10);
}
function weekDay(key) { return new Date(`${key}T12:00:00Z`).getUTCDay(); }
export function isWorkday(key) {const d=weekDay(key);return d>=1&&d<=5;}
export function isFriday(key) { return weekDay(key)===5; }
export function displayDate(key) {if(!key)return '—';const [y,m,d]=key.slice(0,10).split('-');return `${d}/${m}/${y}`;}
export function displayMoney(v) {return Number(v||0).toLocaleString('vi-VN')+' đ';}
export function mealTimeLock(date, deadline, now, today = todayKey(now), tomorrow = dateShift(today, 1)) {
  if (date <= today) return 'Ngày hiện tại hoặc đã qua.';
  if (!deadline || date !== tomorrow) return '';
  const [hour, minute] = deadline.split(':').map(Number);
  return minuteVN(now) >= hour * 60 + minute ? 'Đã đến hạn ' + deadline.slice(0, 5) + ' ngày liền trước.' : '';
}

export function specialFridayWindow(key, settings, now = new Date()) {
  return isFriday(key) && !mealTimeLock(key, settings.cutoff, now);
}
export function adminCancelable(key, now = new Date()) {return key>todayKey(now);}
