import { useState } from 'react';
import RequestFeedback from '../../components/RequestFeedback';
import { useRequestData } from '../../hooks/useRequestData';
import { useToast } from '../../hooks/useToast';
import { nextWorkday } from './ticketDates';
import { cancelOffer, createSellOffer, getExchangeBoard, matchOffer } from '../../services/ticketService';
import { todayKey, dateShift, isFriday, displayDate, displayMoney } from '../../utils/mealDates';
import { TicketPageFrame, ticketTypeLabel, TransferTable } from './TicketDisplay';

const isOpen = offer => ['OPEN', 'PARTIALLY_FILLED'].includes(offer.status) && offer.remaining > 0;
function quotedTotal(offer, count) {
  if (offer.remainingPrices?.length >= count) {
    return offer.remainingPrices.slice(0, count).reduce((sum, value) => sum + Number(value), 0);
  }
  return offer.price == null ? null : Number(offer.price) * count;
}
function offerPriceLabel(offer) {
  const mixedPrices = new Set(offer.remainingPrices ?? []).size > 1;
  if (mixedPrices) return 'Theo giá từng vé';
  return offer.price == null ? 'Theo giá vé' : `${displayMoney(offer.price)} / vé`;
}

export default function ExchangePage() {
  const { notify } = useToast();
  const board = useRequestData(getExchangeBoard);
  const [date, setDate] = useState(nextWorkday);
  const [type, setType] = useState('NORMAL');
  const [quantity, setQuantity] = useState(1);
  const [requested, setRequested] = useState({});
  const [busy, setBusy] = useState(false);
  const available = (board.data?.ownedTickets ?? []).filter(ticket =>
    ticket.mealDate === date && ticket.ticketType === type && ticket.status === 'AVAILABLE');
  const myListings = (board.data?.mine ?? []).filter(isOpen);
  const mine = new Set(myListings.map(item => item.id));
  const offers = (board.data?.offers ?? []).filter(item => item.side === 'SELL' && isOpen(item) && !mine.has(item.id));

  async function run(action, message) {
    if (busy) return;
    setBusy(true);
    try {
      await action();
      notify(message, 'success');
    } catch (error) {
      notify(error.message, 'error');
    } finally {
      board.reload();
      setBusy(false);
    }
  }

  function submit(event) {
    event.preventDefault();
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > Math.min(available.length, 30)) {
      notify('Số lượng không hợp lệ hoặc vượt số vé còn có thể bán.', 'error');
      return;
    }
    run(() => createSellOffer({ mealDate: date, ticketType: type, quantity }), 'Đã đăng bán vé.');
  }

  function buy(offer) {
    const count = Number(requested[offer.id] ?? 1);
    if (!Number.isInteger(count) || count < 1 || count > offer.remaining) {
      notify('Số lượng không hợp lệ.', 'error');
      return;
    }
    const quote = quotedTotal(offer, count);
    const total = quote == null ? '' : ` với tổng tiền ${displayMoney(quote)}`;
    if (!window.confirm(`Nhận ${count} vé ngày ${displayDate(offer.mealDate)} từ ${offer.userName}${total}?`)) return;
    run(async () => {
      await matchOffer(offer.id, count);
      setRequested(previous => ({ ...previous, [offer.id]: 1 }));
    }, `Đã nhận ${count} vé. Chi phí đã cập nhật vào thống kê của bạn.`);
  }

  return <TicketPageFrame title="Chuyển nhượng vé" subtitle="Người có vé đăng bán, đồng nghiệp chọn số lượng và xác nhận nhận vé.">
    <p className="preview-note">Khi chuyển nhượng hoàn tất, vé và chi phí chuyển sang người nhận. Bạn có thể hủy phần chưa được nhận.</p>
    <RequestFeedback request={board} loadingText="Đang tải chợ vé..." />
    <section className="preview-market-sell" aria-label="Đăng bán vé">
      <h3>Đăng bán vé của tôi</h3>
      <form className="preview-form" onSubmit={submit}>
        <label>Ngày ăn<input type="date" min={dateShift(todayKey(), 1)} value={date} onChange={event => { setDate(event.target.value); setType('NORMAL'); setQuantity(1); }} required /></label>
        <label>Loại vé<select value={type} onChange={event => { setType(event.target.value); setQuantity(1); }}>
          <option value="NORMAL">Suất thường</option>{isFriday(date) && <option value="SPECIAL">Suất đặc biệt</option>}
        </select></label>
        <label>Số vé muốn bán <small>Hiện có {board.data ? available.length : '—'} vé chưa đăng bán</small>
          <input type="number" min="1" max={Math.max(1, Math.min(available.length, 30))} step="1" value={quantity} onChange={event => setQuantity(event.target.valueAsNumber || '')} required />
        </label>
        <div className="preview-form__action"><button className="button button--blue" disabled={busy || !available.length}>Đăng bán</button></div>
      </form>
    </section>
    <h3>Vé đồng nghiệp đang bán</h3>
    <div className="preview-market-list">
      {offers.map(offer => <article className="preview-market-card" key={offer.id}>
        <div><strong>{offer.userName}</strong><p>{displayDate(offer.mealDate)} · {ticketTypeLabel(offer.ticketType)}</p></div>
        <div className="preview-market-details"><b>{offer.remaining} vé còn</b><strong>{offerPriceLabel(offer)}</strong></div>
        <label>Nhận bao nhiêu vé?<input type="number" min="1" max={offer.remaining} step="1" value={requested[offer.id] ?? 1}
          onChange={event => setRequested(previous => ({ ...previous, [offer.id]: event.target.value }))} /></label>
        <button className="button button--cyan" disabled={busy} onClick={() => buy(offer)}>Nhận vé</button>
      </article>)}
      {board.data && !offers.length && <p className="preview-empty">Chưa có đồng nghiệp nào đăng bán vé.</p>}
    </div>
    <h3>Vé tôi đang đăng bán</h3>
    <div className="preview-market-list">
      {myListings.map(offer => <article key={offer.id} className="preview-market-card">
        <div><strong>{displayDate(offer.mealDate)}</strong><p>{ticketTypeLabel(offer.ticketType)} · còn {offer.remaining}/{offer.quantity} vé</p></div>
        <button className="preview-outlined" disabled={busy} onClick={() => {
          if (window.confirm('Hủy phần vé chưa được nhận trong bài bán này?')) {
            run(() => cancelOffer(offer.id), 'Đã hủy số vé chưa được nhận.');
          }
        }}>Hủy bài bán</button>
      </article>)}
      {board.data && !myListings.length && <p className="preview-empty">Bạn chưa có bài bán nào đang mở.</p>}
    </div>
    <h3>Lịch sử chuyển vé</h3>
    {board.data && <TransferTable rows={board.data.transactions} />}
  </TicketPageFrame>;
}
