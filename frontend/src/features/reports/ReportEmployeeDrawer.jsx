import { displayDate, displayMoney } from '../../utils/mealDates';
import { summarizeTickets } from './reportSelectors';
import { sourceName } from './reportWorkbook';

export default function ReportEmployeeDrawer({ selectedEmployee, employeeId, employeeTickets, transactions, departmentNames, employeeName, onClose }) {
  return (
    <div className="rv4-backdrop" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) onClose(); }}>
      <section className="rv4-drawer" role="dialog" aria-modal="true" aria-label={`Chi tiết vé ${selectedEmployee.name}`}>
        <header><div><small>CHI TIẾT THEO NHÂN VIÊN</small><h2>{selectedEmployee.name}</h2><p>{selectedEmployee.code} · {departmentNames.get(String(selectedEmployee.departmentId)) || '—'}</p></div><button type="button" aria-label="Đóng chi tiết" onClick={() => setEmployeeId('')}>×</button></header>
        <div className="rv4-drawer-body"><div className="rv4-mini-kpis"><div><small>Vé đang sở hữu</small><strong>{employeeTickets.length}</strong></div><div><small>Tổng tiền</small><strong>{displayMoney(summarizeTickets(employeeTickets).amount)}</strong></div></div>
          <h3>Danh sách vé theo ngày</h3>
          <div className="rv4-table-viewport"><table className="rv4-table"><thead><tr><th>Ngày</th><th>Mã vé</th><th>Loại</th><th>Nguồn</th><th>Giá</th></tr></thead><tbody>{employeeTickets.map(t => <tr key={t.id}><td>{displayDate(t.date)}</td><td>{t.code ?? t.id}</td><td>{t.type === 'SPECIAL' ? 'Đặc biệt' : 'Thường'}</td><td>{sourceName(t)}</td><td>{displayMoney(t.amount)}</td></tr>)}{!employeeTickets.length && <tr><td colSpan={5}>Không có vé phù hợp.</td></tr>}</tbody></table></div>
          <h3>Lịch sử nhận / nhượng vé trong kỳ</h3>
          <div className="rv4-table-viewport"><table className="rv4-table"><thead><tr><th>Ngày ăn</th><th>Vai trò</th><th>Đối tác</th><th>SL</th><th>Giá trị</th></tr></thead><tbody>{transactions.map(x => {const incoming = String(x.toId) === employeeId;return <tr key={x.id}><td>{displayDate(x.date)}</td><td>{incoming ? 'Nhận vé' : 'Nhượng vé'}</td><td>{employeeName(incoming ? x.fromId : x.toId)}</td><td>{x.quantity || 1}</td><td>{displayMoney(x.amount)}</td></tr>;})}{!transactions.length && <tr><td colSpan={5}>Chưa có giao dịch hoàn tất.</td></tr>}</tbody></table></div>
          <p className="rv4-help">Tiền trên báo cáo là giá trị vé hợp lệ hiện đang sở hữu. Lịch sử giao dịch không được cộng thêm một lần nữa.</p>
        </div>
      </section>
    </div>
  );
}
