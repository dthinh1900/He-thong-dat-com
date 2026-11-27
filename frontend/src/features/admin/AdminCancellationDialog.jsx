import { useId } from 'react';
import ModalDialog from '../../components/ModalDialog';

export default function AdminCancellationDialog({ date, employeeName, busy, onClose, onConfirm }) {
  const reasonInput = useId();
  return (
    <ModalDialog title={'Hủy vé ăn ngày ' + date} onClose={onClose} busy={busy}>
      {() => <form className="cancellation-form" onSubmit={event => {
        event.preventDefault();
        if (!busy) onConfirm(new FormData(event.currentTarget).get('reason').trim());
      }}>
        {employeeName && <p className="app-dialog__note">Nhân viên {employeeName}</p>}
        <p className="app-dialog__note">Vé đã hủy sẽ được loại khỏi tổng tiền ăn. Nhân viên có thể xem lý do trên lịch.</p>
        <label htmlFor={reasonInput}>Lý do hủy (không bắt buộc)</label>
        <textarea id={reasonInput} name="reason" rows={3} maxLength={500} disabled={busy} />
        <div className="app-dialog__actions">
          <button type="submit" className="admin-button admin-button--danger" disabled={busy}>
            {busy ? 'Đang hủy…' : 'Xác nhận hủy vé'}
          </button>
        </div>
      </form>}
    </ModalDialog>
  );
}
