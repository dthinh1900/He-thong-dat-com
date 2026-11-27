import ModalDialog from '../../components/ModalDialog';
import { formatMoney } from '../../utils/format';
import { displayDate } from '../../utils/mealDates';

export default function CancelledTicketsDialog({ days, date, onClose, onToggleDate, selected }) {
  const day = date ? days.find(entry => entry.mealDate === date) : null;
  const shown = date ? days.filter(entry => entry.mealDate === date) : days;

  return (
    <ModalDialog title={date ? 'Vé đã hủy ngày ' + displayDate(date) : 'Vé đã hủy trong tháng'} onClose={onClose}>
      {close => <>
        <p className="app-dialog__note">Vé đã hủy không tính vào số suất và tổng tiền ăn. Vé còn hiệu lực trong cùng ngày vẫn tính theo giá ghi trên vé.</p>
        {shown.length === 0 && <p>Tháng này chưa có vé đã hủy.</p>}
        {shown.map(entry => (
          <section className="cancelled-tickets__day" key={entry.mealDate}>
            {!date && <h3>Ngày ăn {displayDate(entry.mealDate)}</h3>}
            {entry.cancelledTickets.map(ticket => (
              <dl className="cancelled-tickets__ticket" key={ticket.ticketId}>
                <dt>Vé</dt><dd>{ticket.ticketCode} · {ticket.ticketType === 'SPECIAL' ? 'Suất đặc biệt' : 'Suất thường'}</dd>
                <dt>Người hủy</dt><dd>Quản trị viên</dd>
                <dt>Thời điểm hủy</dt><dd>{ticket.cancelledAt
                  ? displayDate(ticket.cancelledAt.slice(0, 10)) + ' lúc ' + ticket.cancelledAt.slice(11, 19) + ' (giờ Việt Nam)'
                  : 'Chưa ghi thời điểm'}</dd>
                <dt>Lý do</dt><dd>{ticket.reason || 'Chưa ghi lý do.'}</dd>
                <dt>Giá đã ghi</dt><dd>{formatMoney(ticket.price)}</dd>
                <dt>Tiền tính cho vé</dt><dd>{formatMoney(0)}</dd>
              </dl>
            ))}
          </section>
        ))}
        {day && <div className="app-dialog__actions">
          {day.editable ? <button type="button" className="button button--blue" onClick={() => {
            close();
            onToggleDate(date);
          }}>{selected ? 'Bỏ chọn ngày này' : 'Đăng ký lại ngày này'}</button>
            : <p className="app-dialog__note">{day.lockReason}</p>}
        </div>}
      </>}
    </ModalDialog>
  );
}
