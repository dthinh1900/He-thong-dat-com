import { useState } from 'react';
import { dateShift, displayDate, displayMoney, specialFridayWindow } from '../../utils/mealDates';

export default function FridaySpecialPanel({ activeDate, now, selectedSet, selectedTypes, dayMap, cutoffTime, saving, onChoose, onViewMenu }) {
  const [preferredDate, setPreferredDate] = useState('');
  const cutoff = cutoffTime.slice(0, 5);
  const fridays = Object.keys(dayMap).filter(date => specialFridayWindow(date, { cutoff }, now)).sort();
  const friday = fridays.includes(activeDate) ? activeDate : (fridays.includes(preferredDate) ? preferredDate : '');
  if (!friday) return null;
  const day = dayMap[friday];
  const enabled = day.typeEditable ?? day.editable;
  const selected = selectedSet.has(friday);
  const special = selected && selectedTypes[friday] === 'SPECIAL';
  const { normalPrice, specialPrice } = day;
  return <section className="friday-special-panel" aria-label="Suất ăn đặc biệt thứ Sáu">
    <div className="friday-special-panel__heading"><strong>Suất thứ Sáu</strong></div>
    <div className="friday-special-panel__dish"><div>
      {fridays.length > 1 ? <label>Ngày ăn<select value={friday} onChange={event => setPreferredDate(event.target.value)}>
        {fridays.map(date => <option key={date} value={date}>{displayDate(date)}</option>)}
      </select></label> : <b>{displayDate(friday)}</b>}
      <small>Chọn trước {cutoff} thứ Năm, {displayDate(dateShift(friday, -1))}</small>
    </div></div>
    <div className="friday-special-panel__cost"><span>Suất thường <b>{normalPrice == null ? '—' : displayMoney(normalPrice)}</b></span><span>Suất đặc biệt <strong>{specialPrice == null ? '—' : displayMoney(specialPrice)}</strong></span></div>
    {enabled && <div className="friday-special-panel__buttons"><button type="button" className={special ? 'preview-outlined' : 'button button--cyan'} disabled={saving || normalPrice == null} onClick={() => onChoose(friday, 'NORMAL')}>{selected && !special ? '✓ Suất thường' : 'Chọn suất thường'}</button><button type="button" className={`button button--special${special ? ' is-selected' : ''}`} disabled={saving || specialPrice == null} onClick={() => onChoose(friday, 'SPECIAL')}>{special ? '✓ Đã chọn đặc biệt' : 'Chọn suất đặc biệt'}</button></div>}
    {enabled && specialPrice == null && <p>Chưa có giá áp dụng cho suất đặc biệt.</p>}
    {!enabled && <p>Ngày này không thể đổi loại suất.</p>}
    <button type="button" className="friday-special-panel__menu" onClick={() => onViewMenu(friday)}>Xem ảnh thực đơn tuần ↗</button>
    <p className="friday-special-panel__foot">Nhấn <b>Lưu đăng ký</b> để xác nhận.</p>
  </section>;
}
