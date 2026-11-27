import { useCallback, useEffect, useState } from 'react';
import { useToast } from '../../hooks/useToast';
import { createMealPrice, getMealPrices } from '../../services/adminService';
import { todayKey } from '../../utils/mealDates';
import { dateLabel, money } from './adminFormatting';

export default function AdminMealPrices({ active, loading }) {
  const { notify } = useToast();
  const [mealPrices, setMealPrices] = useState([]);
  const [priceDrafts, setPriceDrafts] = useState({});
  const [savingPriceType, setSavingPriceType] = useState('');

  const loadMealPrices = useCallback(async () => {
    try {
      const rows = await getMealPrices();
      setMealPrices(rows);
      const nextDrafts = {};
      const today = todayKey();
      for (const type of ['NORMAL', 'SPECIAL']) {
        const schedule = rows.filter(row => row.ticketType === type)
          .sort((a, b) => b.effectiveFrom.localeCompare(a.effectiveFrom));
        const current = schedule.find(row => row.effectiveFrom <= today && (!row.effectiveTo || row.effectiveTo >= today));
        nextDrafts[type] = {
          price: current ? String(current.price) : '',
          effectiveFrom: today,
        };
      }
      setPriceDrafts(nextDrafts);
    } catch (error) {
      notify(error.message || 'Không tải được bảng giá vé.', 'error');
    }
  }, [notify]);

  useEffect(() => {
    if (active) loadMealPrices();
  }, [active, loadMealPrices]);

  async function saveMealPrice(type) {
    const draft = priceDrafts[type];
    const value = Number(draft?.price);
    if (!Number.isFinite(value) || value <= 0) {
      notify('Giá vé phải lớn hơn 0.', 'error');
      return;
    }
    if (!draft?.effectiveFrom || draft.effectiveFrom < todayKey()) {
      notify('Chọn ngày áp dụng từ hôm nay trở đi.', 'error');
      return;
    }
    setSavingPriceType(type);
    try {
      await createMealPrice({ ticketType: type, price: value, effectiveFrom: draft.effectiveFrom });
      notify(`Đã lưu giá ${type === 'NORMAL' ? 'vé thường' : 'vé đặc biệt'}.`, 'success');
      await loadMealPrices();
    } catch (error) {
      notify(error.message || 'Không lưu được giá vé.', 'error');
    } finally {
      setSavingPriceType('');
    }
  }

  if (!active) return null;

  return (
    <>
    <section className="admin-price-card">
      <div className="admin-price-card__heading">
        <div><h3>Quản lý giá vé ăn</h3><p>Thiết lập giá cho hai loại vé. Giá mới sẽ áp dụng từ ngày đã chọn và không thay đổi các vé đã phát hành trước đó.</p></div>
      </div>
      <div className="admin-table-wrap">
        <table className="admin-table admin-price-table">
          <thead><tr><th>Loại vé</th><th>Giá hiện tại</th><th>Giá mới (VNĐ)</th><th>Áp dụng từ ngày</th><th>Thao tác</th></tr></thead>
          <tbody>{[
            { type: 'NORMAL', label: 'Vé thường' },
            { type: 'SPECIAL', label: 'Vé đặc biệt' },
          ].map(({ type, label }) => {
            const today = todayKey();
            const schedule = mealPrices.filter(row => row.ticketType === type)
              .sort((a, b) => b.effectiveFrom.localeCompare(a.effectiveFrom));
            const activePrice = schedule.find(row => row.effectiveFrom <= today && (!row.effectiveTo || row.effectiveTo >= today));
            const future = schedule.filter(row => row.effectiveFrom > today).at(-1);
            const draft = priceDrafts[type] || { price: '', effectiveFrom: today };
            return <tr key={type}>
              <td><b>{label}</b></td>
              <td><strong>{activePrice ? money(activePrice.price) : 'Chưa cấu hình'}</strong>
                {activePrice && <small className="admin-cell-subtext">Hiệu lực từ {dateLabel(activePrice.effectiveFrom)}</small>}
                {future && <small className="admin-cell-subtext">{money(future.price)} từ {dateLabel(future.effectiveFrom)}</small>}</td>
              <td><input aria-label={`Giá mới ${label.toLowerCase()}`} className="admin-price-input" type="number" min="0.01" max="9999999999.99" step="0.01" value={draft.price} onChange={(event) => setPriceDrafts((current) => ({ ...current, [type]: { ...draft, price: event.target.value } }))} /></td>
              <td><input aria-label={`Ngày áp dụng ${label.toLowerCase()}`} className="admin-price-input admin-price-date" type="date" min={today} value={draft.effectiveFrom} onChange={(event) => setPriceDrafts((current) => ({ ...current, [type]: { ...draft, effectiveFrom: event.target.value } }))} /></td>
              <td><button type="button" className="admin-button admin-button--primary" disabled={savingPriceType !== '' || loading} onClick={() => saveMealPrice(type)}>{savingPriceType === type ? 'Đang lưu...' : 'Lưu giá'}</button></td>
            </tr>;
          })}</tbody>
        </table>
      </div>
      <p className="admin-price-card__footnote">Lịch sử giá cũ được giữ lại để bảo toàn số tiền của các vé đã đăng ký. Giá mới chỉ áp dụng từ ngày hiệu lực trở đi.</p>
    </section>
    </>
  );
}
