import React from 'react';
import { useCallback, useState } from 'react';
import RequestFeedback from '../../components/RequestFeedback';
import AdminCancellationDialog from '../admin/AdminCancellationDialog';
import { useRequestData } from '../../hooks/useRequestData';
import { useToast } from '../../hooks/useToast';
import {
  cancelTicketAsAdmin, decidePurchase, getEmployeeTicketHistory, listAdminPurchases, listExchangeAudit, listTransfers,
} from '../../services/ticketService';
import { adminCancelable, displayDate, displayMoney, todayKey } from '../../utils/mealDates';
import { Status, ticketTypeLabel, TransferTable } from './TicketDisplay';

function PurchaseReview() {
  const queue = useRequestData(listAdminPurchases);
  const { notify } = useToast();
  const [busy, setBusy] = useState(false);

  async function review(item, decision) {
    if (busy) return;
    const approved = decision === 'APPROVED';
    if (!window.confirm(`${approved ? 'Duyệt' : 'Từ chối'} yêu cầu ${item.quantity} vé của ${item.userName}?`)) return;
    setBusy(true);
    try {
      await decidePurchase(item.id, decision);
      notify(approved ? 'Đã duyệt và phát hành vé.' : 'Đã từ chối yêu cầu.', 'success');
    } catch (error) {
      notify(error.message, 'error');
    } finally {
      queue.reload();
      setBusy(false);
    }
  }

  return <section className="admin-table-card preview-admin-section">
    <h3>Yêu cầu mua thêm vé</h3>
    <div className="preview-admin-section__body">
      <p className="preview-note">Vé được phát hành và tính tiền sau khi duyệt.</p>
      <RequestFeedback request={queue} />
      {queue.data && <div className="preview-table-wrap"><table className="preview-table">
        <thead><tr><th>Nhân viên</th><th>Ngày ăn</th><th>Loại vé</th><th>SL</th><th>Thành tiền</th><th>Lý do</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
        <tbody>{queue.data.map(item => <tr key={item.id}>
          <td>{item.userName}</td><td>{displayDate(item.mealDate)}</td><td>{ticketTypeLabel(item.ticketType)}</td>
          <td>{item.quantity}</td><td>{item.totalAmount == null ? '—' : displayMoney(item.totalAmount)}</td>
          <td>{item.reason}</td><td><Status value={item.status} /></td>
          <td>{item.status === 'PENDING' ? <div className="preview-actions">
            <button type="button" className="preview-link" disabled={busy || item.mealDate < todayKey()} onClick={() => review(item, 'APPROVED')}>Duyệt</button>
            <button type="button" className="preview-link" disabled={busy} onClick={() => review(item, 'REJECTED')}>Từ chối</button>
          </div> : '—'}</td>
        </tr>)}</tbody>
      </table>{!queue.data.length && <p className="preview-empty">Chưa có yêu cầu.</p>}</div>}
    </div>
  </section>;
}

const auditLabels = {
  CREATE_EXCHANGE: 'Đăng bán',
  COMPLETE_EXCHANGE: 'Mua',
  CANCEL_EXCHANGE: 'Hủy đăng bán',
};
function groupExchangeAudit(rows) {
  const groups = new Map();
  rows.forEach(row => {
    const key = row.ticketId == null ? `row-${row.id}` : `ticket-${row.ticketId}`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(row);
  });
  const blocks = [...groups.values()].map(items => {
    items.sort((a, b) => {
      const rank = action => action === 'COMPLETE_EXCHANGE' ? 0 : action === 'CANCEL_EXCHANGE' ? 1 : 2;
      return rank(a.action) - rank(b.action) || b.id - a.id;
    });
    return items;
  });
  blocks.sort((a, b) => {
    const latest = block => Math.max(...block.map(row => new Date(row.createdAt ?? 0).getTime()));
    return latest(b) - latest(a);
  });
  return blocks;
}

function Transfers() {
  const transfers = useRequestData(listTransfers);
  const audit = useRequestData(listExchangeAudit);
  const auditBlocks = audit.data ? groupExchangeAudit(audit.data) : [];
  return <>
    <section className="admin-table-card preview-admin-section">
      <h3>Lịch sử trao đổi vé</h3>
      <div className="preview-admin-section__body">
        <RequestFeedback request={transfers} />
        {transfers.data && <TransferTable rows={transfers.data} admin />}
      </div>
    </section>
    <section className="admin-table-card preview-admin-section">
      <h3>Nhật ký chợ vé</h3>
      <div className="preview-admin-section__body">
        <RequestFeedback request={audit} />
        {audit.data && <div className="preview-table-wrap"><table className="preview-table">
          <thead><tr><th>Thời điểm</th><th>Người thực hiện</th><th>Thao tác</th><th>Nội dung</th></tr></thead>
          <tbody>{auditBlocks.map((block, blockIndex) => <React.Fragment key={`audit-block-${blockIndex}`}>
            {blockIndex > 0 && <tr className="preview-audit-spacer" aria-hidden="true"><td colSpan="4" /></tr>}
            {block.map(row => <tr key={row.id}>
              <td>{row.createdAt ? `${displayDate(row.createdAt)} ${row.createdAt.slice(11, 16)}` : '—'}</td>
              <td>{row.actorName}</td><td>{auditLabels[row.action] ?? row.action}</td><td>{row.description}</td>
            </tr>)}
          </React.Fragment>)}</tbody>
        </table>{!audit.data.length && <p className="preview-empty">Chưa có thao tác.</p>}</div>}
      </div>
    </section>
  </>;
}

export default function AdminTicketTransactions({ tab }) {
  if (tab === 'purchases') return <PurchaseReview />;
  if (tab === 'transfers') return <Transfers />;
  return null;
}

export function AdminEmployeeHistory({ employeeId, onChanged }) {
  const { notify } = useToast();
  const [month, setMonth] = useState(() => todayKey().slice(0, 7));
  const [cancellation, setCancellation] = useState(null);
  const [busy, setBusy] = useState(false);
  const loadHistory = useCallback(() => {
    const [year, monthNumber] = month.split('-').map(Number);
    return getEmployeeTicketHistory(employeeId, monthNumber, year);
  }, [employeeId, month]);
  const history = useRequestData(employeeId && month ? loadHistory : null);

  async function remove(ticket, reason) {
    setBusy(true);
    try {
      await cancelTicketAsAdmin(ticket.ticketId, reason, Boolean(ticket.withdrawable));
      setCancellation(null);
      notify(ticket.withdrawable ? 'Đã bỏ chọn suất ăn.' : 'Đã hủy vé ăn.', 'success');
      onChanged?.();
    } catch (error) {
      notify(error.message, 'error');
    } finally {
      setBusy(false);
      history.reload();
    }
  }

  if (!employeeId) return null;
  return <section className="preview-employee-history">
    <h3>Lịch ăn cá nhân và chi phí</h3>
    <div className="preview-filters">
      <label>Tháng<input type="month" value={month} onChange={event => setMonth(event.target.value)} /></label>
      {history.data && <strong>{history.data.totalTickets} vé · {displayMoney(history.data.totalAmount)}</strong>}
    </div>
    <RequestFeedback request={history} />
    {history.data && <div className="preview-table-wrap"><table className="preview-table">
      <thead><tr><th>Ngày ăn</th><th>Loại vé</th><th>Giá</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
      <tbody>{history.data.tickets.map(ticket => {
        const canCancel = ticket.cancellable ?? (ticket.status === 'AVAILABLE' && adminCancelable(ticket.mealDate));
        return <tr key={ticket.ticketId}><td>{displayDate(ticket.mealDate)}</td>
          <td>{ticketTypeLabel(ticket.ticketType)}</td><td>{displayMoney(ticket.price)}</td>
          <td><Status value={ticket.status} /></td><td><button className="preview-link" type="button" disabled={busy || !canCancel} onClick={() => {
            if (!ticket.withdrawable) setCancellation(ticket);
            else if (window.confirm('Bỏ chọn suất ngày ' + displayDate(ticket.mealDate) + '?')) remove(ticket);
          }}>{ticket.withdrawable ? 'Bỏ chọn' : 'Hủy suất'}</button></td>
        </tr>;
      })}</tbody>
    </table>{!history.data.tickets.length && <p className="preview-empty">Chưa có vé trong tháng này.</p>}</div>}
    {cancellation && <AdminCancellationDialog date={displayDate(cancellation.mealDate)} busy={busy}
      onClose={() => setCancellation(null)} onConfirm={reason => remove(cancellation, reason)} />}
  </section>;
}
