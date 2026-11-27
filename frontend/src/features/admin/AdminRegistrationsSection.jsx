import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import CompactNavigation from '../../components/CompactNavigation';
import AdminCancellationDialog from './AdminCancellationDialog';
import { useToast } from '../../hooks/useToast';
import { getDepartments, getRegistrationSettings, getRegistrations, setRegistration, updateRegistrationSettings }
  from '../../services/adminService';
import { todayKey } from '../../utils/mealDates';
import AdminTicketTransactions from '../tickets/AdminTicketTransactions';
import { dateLabel, sortVietnamese } from './adminFormatting';
import { writeRegistrationWorkbook } from './registrationWorkbook';

const EMPTY_ROSTER = [];

const ticketTabs = [
  ['roster', 'Danh sách đăng ký'],
  ['purchases', 'Duyệt mua thêm'],
  ['transfers', 'Lịch sử trao đổi'],
  ['rules', 'Quy định'],
];

export default function AdminRegistrationsSection({ active, ticketTab, setTicketTab, departments, setDepartments,
  loading, setLoading, saving, setSaving, onRegistrationChanged }) {
  const { notify } = useToast();
  const [onlyRegistered, setOnlyRegistered] = useState(true);
  const [departmentFilter, setDepartmentFilter] = useState('all');
  const [registrationDate, setRegistrationDate] = useState(todayKey);
  const [roster, setRoster] = useState(null);
  const registrationRows = roster?.date === registrationDate && roster.department === departmentFilter
    ? roster.rows : EMPTY_ROSTER;
  const [cancellation, setCancellation] = useState(null);
  const registrationRequest = useRef(0);
  const [cutoff, setCutoff] = useState('14:45');
  const [cancelDeadline, setCancelDeadline] = useState('14:45');

  const loadRegistrations = useCallback(async () => {
    const request = ++registrationRequest.current;
    if (!registrationDate) {
      setRoster(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const selectedDepartmentId = departmentFilter === 'all' ? undefined : departmentFilter;
      const [rows, settings, departmentRows] = await Promise.all([
        getRegistrations(registrationDate, selectedDepartmentId),
        getRegistrationSettings(),
        getDepartments(),
      ]);
      if (request !== registrationRequest.current) return;
      setRoster({ date: registrationDate, department: departmentFilter, rows });
      setCutoff(String(settings.cutoff ?? '14:45').slice(0, 5));
      setCancelDeadline(String(settings.cancelDeadline ?? '14:45').slice(0, 5));
      setDepartments(departmentRows);
    } catch (error) {
      if (request === registrationRequest.current) notify(error.message || 'Không tải được danh sách đăng ký ăn.', 'error');
    } finally {
      if (request === registrationRequest.current) setLoading(false);
    }
  }, [departmentFilter, registrationDate, notify, setDepartments, setLoading]);

  useEffect(() => {
    if (active && (ticketTab === 'roster' || ticketTab === 'rules')) loadRegistrations();
  }, [active, ticketTab, loadRegistrations]);

  function toggleRegistration(row) {
    if (row.registered) {
      if (!row.withdrawable) setCancellation(row);
      else if (window.confirm('Bỏ chọn suất ngày ' + dateLabel(registrationDate) + ' cho ' + row.name + '?')) {
        updateRegistration(row, false, null, true);
      }
    } else if (window.confirm('Xác nhận đăng ký ăn cho ' + row.name + ' ngày ' + dateLabel(registrationDate) + '?')) {
      updateRegistration(row, true);
    }
  }

  async function updateRegistration(row, next, reason, withdrawal) {
    setSaving(true);
    try {
      await setRegistration(row.id, registrationDate, next, reason, withdrawal);
      setCancellation(null);
      notify(next ? 'Đã đăng ký ăn.' : withdrawal ? 'Đã bỏ chọn suất ăn.' : 'Đã hủy vé cơm.', 'success');
      await loadRegistrations();
      await onRegistrationChanged();
    } catch (error) {
      notify(error.message || 'Không cập nhật được đăng ký ăn.', 'error');
    } finally {
      setSaving(false);
    }
  }

  async function saveSettings() {
    setSaving(true);
    try {
      const saved = await updateRegistrationSettings({ cutoff, cancelDeadline });
      setCutoff(String(saved.cutoff).slice(0, 5));
      setCancelDeadline(String(saved.cancelDeadline).slice(0, 5));
      notify('Đã lưu thời hạn đăng ký và hủy vé.', 'success');
      await loadRegistrations();
    } catch (error) {
      notify(error.message || 'Không lưu được cấu hình thời hạn.', 'error');
    } finally {
      setSaving(false);
    }
  }

  const registrationDepartments = useMemo(() => {
    const groups = new Map();
    departments.forEach((department) => groups.set(department.id, { ...department, rows: [] }));
    registrationRows.forEach((row) => {
      if (onlyRegistered && !row.registered) return;
      if (!groups.has(row.departmentId)) {
        groups.set(row.departmentId, {
          id: row.departmentId,
          name: `Phòng ban #${row.departmentId}`,
          rows: [],
        });
      }
      groups.get(row.departmentId).rows.push(row);
    });
    return [...groups.values()].filter((group) => group.rows.length > 0)
      .map((group) => ({ ...group, rows: sortVietnamese(group.rows, 'name') }));
  }, [departments, registrationRows, onlyRegistered]);

  async function exportRegistrations() {
    if (!registrationRows.length) {
      notify('Không có dữ liệu đăng ký để xuất Excel.', 'error');
      return;
    }
    setSaving(true);
    try {
      const count = await writeRegistrationWorkbook({ registrationRows, departments, onlyRegistered, departmentFilter, registrationDate });
      notify('Đã xuất ' + count + ' dòng đăng ký ra Excel.', 'success');
    } catch (error) {
      notify(error.message || 'Không xuất được file Excel.', 'error');
    } finally {
      setSaving(false);
    }
  }

  if (!active) return null;

  return (
    <>
    <CompactNavigation label="Đăng ký và giao dịch" className="admin-ticket-navigation" itemsClassName="preview-subnav"
      currentLabel={ticketTabs.find(([key]) => key === ticketTab)?.[1]}>
      {close => ticketTabs.map(([key, label]) => <button type="button" aria-pressed={ticketTab === key}
        className={ticketTab === key ? 'is-active' : ''} onClick={() => { setTicketTab(key); close(); }} key={key}>{label}</button>)}
    </CompactNavigation>
    {ticketTab === 'roster' && <>
    <div className="admin-section-heading">
      <div><h2>Danh sách đăng ký ăn</h2><p>Xem nhân viên đã đăng ký và chưa đăng ký theo từng phòng.</p></div>
      <label className="admin-inline-field">Ngày đăng ký<input type="date" value={registrationDate} onChange={(event) => setRegistrationDate(event.target.value)} /></label>
    </div>
    <div className="admin-filter-row admin-registration-toolbar">
      <div className="admin-registration-filters">
        <label>Phòng ban<select value={departmentFilter} onChange={(event) => setDepartmentFilter(event.target.value === 'all' ? 'all' : Number(event.target.value))}><option value="all">Tất cả phòng ban</option>{departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}</select></label>
        <label className="preview-check"><input type="checkbox" checked={onlyRegistered} onChange={e => setOnlyRegistered(e.target.checked)} /> Chỉ người đã đăng ký</label>
        <span>{registrationRows.filter((row) => row.registered).length} nhân viên đã đăng ký</span>
      </div>
      <button type="button" className="admin-button admin-button--excel" disabled={saving || loading || !registrationRows.length} onClick={exportRegistrations}>⇩ Xuất Excel</button>
    </div>
    <div className="admin-note">Danh sách này quản lý đăng ký theo tháng. Vé mua thêm và vé nhận chuyển nhượng được xem trong lịch sử của nhân viên.</div>
    {registrationDepartments.map((department) => <section className="admin-table-card" key={department.id}>
      <h3>{department.name}</h3>
      <div className="admin-table-wrap"><table className="admin-table admin-table--roster">
        <thead><tr><th>STT</th><th>Mã nhân viên</th><th>Tên nhân viên</th><th>Vé ăn</th><th>Thao tác</th></tr></thead>
        <tbody>{department.rows.map((row, index) => <tr key={row.id}>
          <td>{index + 1}</td><td>{row.code}</td><td><b>{row.name}</b></td>
          <td><span className={`admin-pill ${row.registered ? 'is-active' : 'is-locked'}`}>{row.registered ? 'Đã đăng ký' : 'Chưa đăng ký'}</span>{row.status && <small className="admin-cell-subtext">{row.status === 'WITHDRAWN' ? 'Đã bỏ chọn' : row.status}</small>}</td>
          <td><button className="admin-text-button" disabled={!row.editable || saving || loading} title={row.lockReason || ''} onClick={() => toggleRegistration(row)}>{row.registered ? (row.withdrawable ? 'Bỏ chọn' : 'Hủy vé cơm') : 'Đăng ký ăn'}</button>{row.lockReason && <small className="admin-cell-subtext">{row.lockReason}</small>}</td>
        </tr>)}</tbody>
      </table></div>
    </section>)}
    {!loading && !registrationDepartments.length && <div className="admin-empty">Không có nhân viên trong bộ lọc này.</div>}
    </>}

    {ticketTab === 'rules' && <div className="admin-settings-card">
      <h3>Thời hạn và quy tắc đăng ký</h3>
      <div className="admin-form-grid">
        <label>Thời điểm chốt đăng ký<input type="time" value={cutoff} onChange={(event) => setCutoff(event.target.value)} /></label>
        <label>Hạn bỏ chọn suất đã đăng ký<input type="time" value={cancelDeadline} onChange={(event) => setCancelDeadline(event.target.value)} /></label>
      </div>
      <p>Áp dụng theo giờ Việt Nam. Hạn bỏ chọn không vượt quá giờ chốt đăng ký. Sau hạn, Admin có thể hỗ trợ hủy vé hợp lệ từ ngày mai.</p>
      <button className="admin-button admin-button--primary" disabled={saving} onClick={saveSettings}>{saving ? 'Đang lưu...' : 'Lưu cấu hình'}</button>
    </div>}
    {['purchases', 'transfers'].includes(ticketTab) && <AdminTicketTransactions tab={ticketTab} />}
    {cancellation && <AdminCancellationDialog date={dateLabel(registrationDate)} employeeName={cancellation.name} busy={saving}
      onClose={() => setCancellation(null)} onConfirm={reason => updateRegistration(cancellation, false, reason, false)} />}
    </>
  );
}
