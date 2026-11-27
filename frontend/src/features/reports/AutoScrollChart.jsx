import { useEffect, useRef } from 'react';
import { displayDate, isWorkday } from '../../utils/mealDates';

/** Horizontal chart: after a 5s interaction pause, scrolling resumes. */
export default function AutoScrollChart({ days, label = 'Biểu đồ số suất theo ngày', onSelectDay, selectedDate }) {
  const viewport = useRef(null);
  const drag = useRef({ active: false, moved: false, startX: 0, startScroll: 0, pointerId: null });
  const onPointerDown = event => {
    if (event.button !== 0) return;
    const node = viewport.current;
    if (!node) return;
    drag.current = { active: true, moved: false, startX: event.clientX, startScroll: node.scrollLeft, pointerId: event.pointerId };
    node.setPointerCapture?.(event.pointerId);
  };
  const onPointerMove = event => {
    const state = drag.current;
    const node = viewport.current;
    if (!state.active || !node) return;
    const delta = event.clientX - state.startX;
    if (Math.abs(delta) > 4) state.moved = true;
    if (state.moved) {
      node.scrollLeft = state.startScroll - delta;
      event.preventDefault();
    }
  };
  const onPointerUp = event => {
    const node = viewport.current;
    if (node?.hasPointerCapture?.(event.pointerId)) node.releasePointerCapture(event.pointerId);
    drag.current.active = false;
    window.setTimeout(() => { drag.current.moved = false; }, 0);
  };
  const preventClickAfterDrag = event => {
    if (drag.current.moved) {
      event.preventDefault();
      event.stopPropagation();
    }
  };
  const workdays = days.filter(day => isWorkday(day.date));
  const length = workdays.length;
  const max = Math.max(1, ...workdays.map(day => Number(day.tickets || 0)));

  useEffect(() => {
    const node = viewport.current;
    if (!node) return;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || length <= 8) return;
    let pauseUntil = Date.now() + 1500;
    let inView = true;
    const pause = () => { pauseUntil = Date.now() + 5000; };
    for (const name of ['wheel', 'pointerdown', 'touchstart', 'keydown']) node.addEventListener(name, pause, { passive: true });
    const observer = 'IntersectionObserver' in window
      ? new IntersectionObserver(entries => { inView = entries[0]?.isIntersecting ?? false; }, { threshold: 0.1 })
      : null;
    observer?.observe(node);
    const timer = window.setInterval(() => {
      if (!inView || document.hidden || Date.now() < pauseUntil || node.scrollWidth <= node.clientWidth + 2) return;
      if (node.scrollLeft + node.clientWidth >= node.scrollWidth - 3) node.scrollLeft = 0;
      else node.scrollLeft += 1;
    }, 35);
    return () => {
      window.clearInterval(timer);
      observer?.disconnect();
      for (const name of ['wheel', 'pointerdown', 'touchstart', 'keydown']) node.removeEventListener(name, pause);
    };
  }, [length]);

  if (!length) return <p className="preview-chart-empty">Không có ngày từ thứ Hai đến thứ Sáu trong kỳ đã chọn.</p>;

  return <div className="preview-chart-scroll" ref={viewport} tabIndex={0} role="region" aria-label={`${label}. Kéo ngang hoặc kéo chuột để xem các ngày khác.`}
    onPointerDown={onPointerDown} onPointerMove={onPointerMove} onPointerUp={onPointerUp} onPointerCancel={onPointerUp}
    onClickCapture={preventClickAfterDrag}>
    <div className="preview-chart-bars">
      {workdays.map(day => <div className={`preview-chart-bar${selectedDate === day.date ? ' is-selected' : ''}`} key={day.date} title={`${displayDate(day.date)}: ${day.tickets} suất`}>
        {onSelectDay ? <button type="button" className="preview-chart-bar-button" aria-pressed={selectedDate === day.date}
          aria-label={`${displayDate(day.date)}, ${day.tickets} vé. Lọc chi tiết ngày này`}
          onClick={() => onSelectDay(day.date)}>
          <span className="preview-chart-value">{Number(day.tickets || 0)}</span>
          <span className="preview-chart-track"><span className="preview-chart-fill" style={{ height: `${Math.max(3, Number(day.tickets || 0) * 100 / max)}%` }} /></span>
          <span className="preview-chart-day">{day.date.slice(-2)}</span>
        </button> : <>
          <span className="preview-chart-value">{Number(day.tickets || 0)}</span>
          <span className="preview-chart-track"><span className="preview-chart-fill" style={{ height: `${Math.max(3, Number(day.tickets || 0) * 100 / max)}%` }} /></span>
          <span className="preview-chart-day">{day.date.slice(-2)}</span>
        </>}
      </div>)}
    </div>
  </div>;
}
