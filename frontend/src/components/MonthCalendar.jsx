import { MONTH_LABELS, WEEKDAY_LABELS } from '../constants/app';
import { getCalendarCells, parseMonthKey } from '../utils/date';
import { todayKey } from '../utils/mealDates';

export default function MonthCalendar({ monthKey, selectedSet, selectedTypes, dayMap, now, saving,
  onToggleDate, onViewCancelled, onPrevMonth, onNextMonth }) {
  const { year, monthIndex } = parseMonthKey(monthKey);
  const today = todayKey(now);
  const displayDay = today.startsWith(monthKey) ? Number(today.slice(-2)) : 1;

  return (
    <section className="month-calendar" aria-label={'Lịch đăng ký tháng ' + (monthIndex + 1) + '/' + year}>
      <div className="month-calendar__hero">
        <div className="month-calendar__checked" aria-hidden="true">✓</div>
        <div className="month-calendar__hero-content">
          <div className="month-calendar__navigation">
            <button type="button" onClick={onPrevMonth} aria-label="Tháng trước">‹</button>
            <span className="month-calendar__big-day">{displayDay}</span>
            <button type="button" onClick={onNextMonth} aria-label="Tháng sau">›</button>
          </div>
          <div className="month-calendar__month-label">{MONTH_LABELS[monthIndex]} - {year}</div>
        </div>
      </div>
      <div className="month-calendar__body">
        <div className="month-calendar__weekdays" aria-hidden="true">
          {WEEKDAY_LABELS.map(day => <span key={day}>{day}</span>)}
        </div>
        <div className="month-calendar__days">
          {getCalendarCells(monthKey).map((cell, index) => {
            if (!cell) return <span key={'blank-' + index} aria-hidden="true" />;
            const date = cell.dateKey;
            const info = dayMap[date];
            const saved = Boolean(info?.selected);
            const selected = selectedSet.has(date);
            const cancelled = Boolean(info?.cancelledTickets?.length);
            const active = selected || (cancelled && (info.ticketCount ?? 0) > 0);
            // The draft selection is authoritative: if a saved ticket is
            // deselected, show it as unregistered even when its saved type was SPECIAL.
            const special = selected && (selectedTypes[date] || info?.ticketType || 'NORMAL') === 'SPECIAL';
            const changed = saved !== selected || (selected && selectedTypes[date] !== info?.ticketType);
            const lockReason = info?.editable ? null : (info?.lockReason || 'Đang tải trạng thái ngày.');
            const locked = !cancelled && Boolean(lockReason);
            const status = selected ? (special ? 'đã chọn suất đặc biệt' : 'đã chọn') : saved ? (special ? 'đã đăng ký suất đặc biệt' : 'đã đăng ký') : 'chưa đăng ký';
            const draft = changed ? (selected ? '. Chọn suất (chưa lưu)' : '. Bỏ chọn (chưa lưu)') : '';
            const classes = ['month-calendar__date',
              active ? 'month-calendar__date--selected' : cancelled && 'month-calendar__date--cancelled',
              locked && 'month-calendar__date--locked', special && 'month-calendar__date--special',
              changed && 'month-calendar__date--draft'].filter(Boolean).join(' ');

            return <button type="button" key={date} className={classes}
              aria-pressed={cancelled ? undefined : selected} aria-haspopup={cancelled ? 'dialog' : undefined}
              disabled={saving || locked}
              aria-label={'Ngày ' + cell.day + '/' + (monthIndex + 1) + '/' + year + ': ' + status
                + (cancelled ? '. Có vé Admin đã hủy' : '') + draft
                + (lockReason ? '. ' + lockReason : '')}
              title={cancelled ? 'Xem vé đã hủy' : lockReason || 'Chọn / bỏ chọn ngày ăn'}
              onClick={() => cancelled ? onViewCancelled(date) : onToggleDate(date)}>
              {cell.day}
              {cancelled && <span className="month-calendar__cancellation-mark" aria-hidden="true">×</span>}
            </button>;
          })}
        </div>
      </div>
    </section>
  );
}
