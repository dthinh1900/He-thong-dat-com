import { useCallback, useEffect, useState } from 'react';
import RequestFeedback from '../../components/RequestFeedback';
import { useRequestData } from '../../hooks/useRequestData';
import { useToast } from '../../hooks/useToast';
import { nextWorkday } from './ticketDates';
import { getPurchaseQuote, listPurchases, requestPurchase } from '../../services/ticketService';
import { todayKey, isFriday, isWorkday, displayDate, displayMoney } from '../../utils/mealDates';
import { Status, TicketPageFrame, ticketTypeLabel } from './TicketDisplay';

export default function PurchasePage() {
  const { notify } = useToast();
  const history = useRequestData(listPurchases);
  const [date, setDate] = useState(nextWorkday);
  const [type, setType] = useState('NORMAL');
  const [quantity, setQuantity] = useState(1);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const validDate = Boolean(date && date >= todayKey() && isWorkday(date));
  const loadPrice = useCallback(() => getPurchaseQuote(date, type), [date, type]);
  const quote = useRequestData(validDate ? loadPrice : null);
  const { reload: reloadQuote } = quote;
  const unitPrice = quote.data?.unitPrice;

  useEffect(() => {
    window.addEventListener('focus', reloadQuote);
    return () => window.removeEventListener('focus', reloadQuote);
  }, [reloadQuote]);

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    if (!isWorkday(date) || date < todayKey()) {
      notify('Vui lòng chọn ngày làm việc từ hôm nay trở đi.', 'error');
      return;
    }
    if (unitPrice == null || !reason.trim()) {
      notify('Kiểm tra giá vé và lý do mua thêm trước khi gửi.', 'error');
      return;
    }
    setBusy(true);
    try {
      await requestPurchase({ mealDate: date, ticketType: type, quantity, reason: reason.trim(), expectedUnitPrice: unitPrice });
      setReason('');
      history.reload();
      notify('Đã gửi yêu cầu mua thêm vé.', 'success');
    } catch (error) {
      notify(error.message, 'error');
      reloadQuote();
    } finally {
      setBusy(false);
    }
  }

  return <TicketPageFrame title="Mua thêm vé" subtitle="Dành cho nhu cầu phát sinh, tiếp khách hoặc quên đăng ký trước giờ khóa sổ.">
    <p className="preview-note">Vé và chi phí được cộng vào lịch sau khi Admin duyệt yêu cầu.</p>
    <RequestFeedback request={quote} loadingText="Đang tải giá vé..." />
    <form className="preview-form" onSubmit={submit} aria-busy={busy}>
      <label>Ngày ăn<input type="date" min={todayKey()} value={date} onChange={event => { setDate(event.target.value); setType('NORMAL'); }} required /></label>
      <label>Loại suất<select value={type} onChange={event => setType(event.target.value)}>
        <option value="NORMAL">Suất thường</option>
        {isFriday(date) && <option value="SPECIAL">Suất đặc biệt</option>}
      </select></label>
      <label>Số lượng vé<input type="number" min="1" max="30" step="1" value={quantity} onChange={event => setQuantity(event.target.valueAsNumber || '')} required /></label>
      <label className="preview-wide">Lý do mua thêm<textarea rows="3" maxLength={1000} value={reason} onChange={event => setReason(event.target.value)} placeholder="Ví dụ: Đặt cơm tiếp khách phòng ban" required /></label>
      <div className="preview-wide">
        {unitPrice != null && <p className="preview-purchase-quote" aria-live="polite">
          Đơn giá hiện hành <strong>{displayMoney(unitPrice)} / vé</strong>
          {Number.isInteger(quantity) && quantity >= 1 && quantity <= 30 && <span>Tổng dự kiến <strong>{displayMoney(unitPrice * quantity)}</strong></span>}
        </p>}
        <button className="button button--blue" disabled={busy || unitPrice == null || !validDate}>{busy ? 'Đang gửi...' : 'Gửi yêu cầu'}</button>
      </div>
    </form>
    <h3>Lịch sử yêu cầu</h3>
    <RequestFeedback request={history} />
    {history.data && <div className="preview-table-wrap"><table className="preview-table">
      <thead><tr><th>Ngày</th><th>Loại</th><th>Số vé</th><th>Thành tiền lúc gửi</th><th>Lý do</th><th>Trạng thái</th></tr></thead>
      <tbody>{history.data.map(item => <tr key={item.id}>
        <td>{displayDate(item.mealDate)}</td><td>{ticketTypeLabel(item.ticketType)}</td><td>{item.quantity}</td>
        <td>{item.totalAmount == null ? '—' : displayMoney(item.totalAmount)}</td><td>{item.reason}</td>
        <td><Status value={item.status} /></td>
      </tr>)}</tbody>
    </table>{!history.data.length && <p className="preview-empty">Chưa có yêu cầu.</p>}</div>}
  </TicketPageFrame>;
}
