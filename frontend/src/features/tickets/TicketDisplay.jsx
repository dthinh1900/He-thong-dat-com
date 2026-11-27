import { useAuth } from '../../hooks/useAuth';
import { displayDate, displayMoney } from '../../utils/mealDates';
import '../../styles/feature-panels.css';

export function TicketPageFrame({ title, subtitle, children }) {
  return <div className="preview-page">
    <h1 className="preview-page__title">{title}</h1>
    <section className="preview-panel">
      <header className="preview-panel__header"><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</header>
      <div className="preview-panel__body">{children}</div>
    </section>
  </div>;
}

export const ticketTypeLabel = type => type === 'SPECIAL' ? 'Suất đặc biệt' : 'Suất thường';

const statusLabels = {
  PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', OPEN: 'Đang đăng bán',
  PARTIALLY_FILLED: 'Đã nhận một phần', MATCHED: 'Đã khớp', CANCELLED: 'Đã hủy', WITHDRAWN: 'Đã bỏ chọn',
  COMPLETED: 'Hoàn tất', EXPIRED: 'Hết hạn', AVAILABLE: 'Hợp lệ', USED: 'Đã sử dụng',
  LISTED_FOR_EXCHANGE: 'Đang đăng bán', PENDING_APPROVAL: 'Chờ duyệt',
};

export function Status({ value }) {
  return <span className={`preview-status preview-status--${value?.toLowerCase()}`}>{statusLabels[value] ?? value}</span>;
}

export function TransferTable({ rows, admin = false }) {
  const { user } = useAuth();
  return <div className="preview-table-wrap"><table className="preview-table">
    <thead><tr>{admin ? <><th>Người bán</th><th>Người nhận</th></> : <th>Vai trò</th>}
      <th>Ngày ăn</th><th>Loại vé</th><th>Số lượng</th><th>Thành tiền</th><th>Trạng thái</th></tr></thead>
    <tbody>{rows.map(item => <tr key={item.id}>
      {admin ? <><td>{item.sellerName}</td><td>{item.buyerName}</td></>
        : <td>{item.sellerId === user?.id ? 'Đã nhượng' : 'Đã nhận'}</td>}
      <td>{displayDate(item.mealDate)}</td><td>{ticketTypeLabel(item.ticketType)}</td><td>{item.quantity ?? 1}</td>
      <td>{displayMoney(item.amount)}</td><td><Status value="COMPLETED" /></td>
    </tr>)}</tbody>
  </table>{!rows.length && <p className="preview-empty">Chưa có giao dịch.</p>}</div>;
}
