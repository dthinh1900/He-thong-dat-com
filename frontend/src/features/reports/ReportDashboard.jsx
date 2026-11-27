import { useEffect, useMemo, useRef, useState } from 'react';
import { useToast } from '../../hooks/useToast';
import { todayKey, displayDate, displayMoney } from '../../utils/mealDates';
import { exportReportRange, getReportDetail } from '../../services/adminService';
import { downloadBlob } from '../../utils/workbook';
import AutoScrollChart from './AutoScrollChart';
import {
  periodBounds, previousBounds, dayCount, selectTickets, summarizeTickets,
  buildDaily, buildDepartments, buildEmployees, transferActivity, employeeTransfers, pctChange,
} from './reportSelectors';
import './report.css';
import ReportEmployeeDrawer from './ReportEmployeeDrawer';
import { exportVisibleDetail } from './reportWorkbook';

const PAGE_SIZE = 10;
const empty = { total: 0, amount: 0, normal: 0, special: 0, purchased: 0, received: 0 };
const emptyReport = { departments: [], employees: [], tickets: [], transactions: [] };
const formatPct = n => n === null ? 'Kỳ trước: 0' : `${n > 0 ? '+' : ''}${n.toFixed(1)}% so với kỳ trước`;
const human = (state, id) => state.employees.find(e => String(e.id) === String(id))?.name || `NV #${id}`;

function KPI({ title, value, helper, tone = '' }) {
  return <article className={`rv4-kpi ${tone}`}><span>{title}</span><strong>{value}</strong><small>{helper}</small></article>;
}

export default function ReportDashboard({ initialDay }) {
  const { notify } = useToast();
  const [mode, setMode] = useState(initialDay ? 'day' : 'month');
  const [month, setMonth] = useState(() => todayKey().slice(0, 7));
  const [day, setDay] = useState(() => initialDay || todayKey());
  const [from, setFrom] = useState(() => `${todayKey().slice(0, 7)}-01`);
  const [to, setTo] = useState(todayKey);
  const [department, setDepartment] = useState('all');
  const [type, setType] = useState('all');
  const [source, setSource] = useState('all');
  const [query, setQuery] = useState('');
  const [selectedDay, setSelectedDay] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [page, setPage] = useState(1);
  const [busy, setBusy] = useState(false);
  const [report, setReport] = useState(null);
  const [retry, setRetry] = useState(0);
  const detailRef = useRef(null);

  const bounds = useMemo(() => periodBounds({ mode, month, day, from, to }), [mode, month, day, from, to]);
  const valid = Boolean(bounds.from >= '2000-01-01' && bounds.to <= '2200-12-31' &&
    bounds.from <= bounds.to && dayCount(bounds.from, bounds.to) <= 366);
  const rangeKey = `${bounds.from}/${bounds.to}/${retry}`;
  const current = report?.key === rangeKey ? report : null;
  const state = current?.data ?? emptyReport;
  const baseFilter = useMemo(() => ({ from: bounds.from, to: bounds.to, department, type, source, query: query.trim() }),
    [bounds.from, bounds.to, department, type, source, query]);
  const tickets = useMemo(() => selectTickets(state, baseFilter), [state, baseFilter]);
  const totals = useMemo(() => summarizeTickets(tickets), [tickets]);
  const prior = useMemo(() => {
    if (!current?.prior) return empty;
    return summarizeTickets(selectTickets(current.prior, { ...baseFilter, ...previousBounds(bounds.from, bounds.to) }));
  }, [current, baseFilter, bounds.from, bounds.to]);
  const days = useMemo(() => valid ? buildDaily(tickets, bounds.from, bounds.to) : [], [tickets, valid, bounds.from, bounds.to]);
  const departments = useMemo(() => buildDepartments(state, tickets), [state, tickets]);
  const departmentNames = useMemo(() => new Map(state.departments.map(d => [String(d.id), d.name])), [state.departments]);
  const dayTickets = useMemo(() => selectedDay ? tickets.filter(t => t.date === selectedDay) : tickets, [tickets, selectedDay]);
  const detailFilter = useMemo(() => selectedDay ? { ...baseFilter, from: selectedDay, to: selectedDay } : baseFilter,
    [baseFilter, selectedDay]);
  const detailRows = useMemo(() => buildEmployees(dayTickets, state, detailFilter), [dayTickets, state, detailFilter]);
  const filteredTickets = useMemo(() => dayTickets.map(t => ({ ...t, departmentName: departmentNames.get(String(t.owner.departmentId)) || '—' }))
    .sort((a, b) => a.date.localeCompare(b.date) || a.owner.name.localeCompare(b.owner.name, 'vi')),
  [dayTickets, departmentNames]);
  const selectedEmployee = detailRows.find(x => String(x.person.id) === employeeId)?.person || null;
  const employeeTickets = selectedEmployee ? filteredTickets.filter(t => String(t.userId) === employeeId) : [];
  const activity = useMemo(() => transferActivity(state, baseFilter), [state, baseFilter]);
  const transactions = selectedEmployee ? employeeTransfers(state, employeeId, detailFilter) : [];
  const totalPages = Math.max(1, Math.ceil(detailRows.length / PAGE_SIZE));
  const visiblePeople = detailRows.slice((Math.min(page, totalPages) - 1) * PAGE_SIZE, Math.min(page, totalPages) * PAGE_SIZE);
  const totalDetail = summarizeTickets(dayTickets);
  const primaryPeriodLabel = `${displayDate(bounds.from)} – ${displayDate(bounds.to)}`;

  useEffect(() => {
    if (!valid) return undefined;
    let active = true;
    const previous = previousBounds(bounds.from, bounds.to);
    Promise.all([
      getReportDetail(bounds.from, bounds.to),
      previous.from >= '2000-01-01' ? getReportDetail(previous.from, previous.to) : null,
    ]).then(([data, prior]) => { if (active) setReport({ key: rangeKey, data, prior }); })
      .catch(error => { if (active) setReport({ key: rangeKey, error: error.message }); });
    return () => { active = false; };
  }, [valid, bounds.from, bounds.to, rangeKey]);

  useEffect(() => { setSelectedDay(''); setEmployeeId(''); setPage(1); }, [mode, month, day, from, to, department, type, source, query]);
  useEffect(() => { setPage(1); setEmployeeId(''); }, [selectedDay]);
  useEffect(() => { if (!employeeId) return; const close = event => { if (event.key === 'Escape') setEmployeeId(''); }; document.addEventListener('keydown', close); return () => document.removeEventListener('keydown', close); }, [employeeId]);

  function focusDay(value) {
    setSelectedDay(current => current === value ? '' : value);
    detailRef.current?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' });
  }
  function focusDepartment(value) {
    setDepartment(String(value));
    setSelectedDay('');
    detailRef.current?.scrollIntoView({ behavior: 'auto', block: 'start' });
  }
  async function downloadAll() {
    if (!valid) return;
    setBusy(true);
    try {
      // Intentionally exports the full date range, independent of the on-screen department/type/search filters.
      const { blob } = await exportReportRange(bounds.from, bounds.to);
      downloadBlob(blob, `bao-cao-${bounds.from}-${bounds.to}.xlsx`);
      notify('Đã xuất báo cáo tổng hợp cả kỳ.', 'success');
    } catch (e) { notify(e.message || 'Không xuất được Excel.', 'error'); }
    finally { setBusy(false); }
  }
  async function downloadCurrent() {
    if (!valid) return;
    setBusy(true);
    try { await exportVisibleDetail(filteredTickets, bounds.from, bounds.to); notify('Đã xuất chi tiết các vé đang lọc.', 'success'); }
    catch (e) { notify(e.message || 'Không xuất được Excel.', 'error'); }
    finally { setBusy(false); }
  }
  return <div className="rv4-root">
    <section className="rv4-toolbar" aria-label="Bộ lọc báo cáo">
      <div className="rv4-heading"><div><h2>Báo cáo & thống kê</h2><p>Toàn cảnh suất ăn, theo dõi phòng ban và mở chi tiết từng nhân viên.</p></div>
        <span className="rv4-period">{primaryPeriodLabel}</span></div>
      <div className="rv4-filters">
        <label>Thời gian<select value={mode} onChange={e => setMode(e.target.value)}><option value="month">Theo tháng</option><option value="day">Theo ngày</option><option value="range">Khoảng ngày</option></select></label>
        {mode === 'month' ? <label>Tháng<input type="month" value={month} onChange={e => setMonth(e.target.value)} /></label> : mode === 'day' ? <label>Ngày<input type="date" value={day} onChange={e => setDay(e.target.value)} /></label> : <><label>Từ ngày<input type="date" value={from} onChange={e => setFrom(e.target.value)} /></label><label>Đến ngày<input type="date" value={to} onChange={e => setTo(e.target.value)} /></label></>}
        <label>Phòng ban<select value={department} onChange={e => setDepartment(e.target.value)}><option value="all">Tất cả phòng ban</option>{state.departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}</select></label>
        <label>Loại vé<select value={type} onChange={e => setType(e.target.value)}><option value="all">Tất cả loại</option><option value="NORMAL">Suất thường</option><option value="SPECIAL">Suất đặc biệt</option></select></label>
        <label>Nguồn vé<select value={source} onChange={e => setSource(e.target.value)}><option value="all">Tất cả nguồn</option><option value="REGISTRATION">Đăng ký tháng</option><option value="PURCHASE">Mua thêm</option><option value="TRANSFER">Nhận chuyển nhượng</option></select></label>
        <label className="rv4-search">Tìm nhân viên<input value={query} onChange={e => setQuery(e.target.value)} placeholder="Tên hoặc mã NV" /></label>
      </div>
      {!valid && <p className="form-message form-message--error" role="alert">Khoảng ngày không hợp lệ hoặc vượt 366 ngày. Hãy chọn lại.</p>}
      <div className="rv4-toolbar-actions"><span>Chỉ tính vé hợp lệ của người đang sở hữu.</span><div><button className="admin-button admin-button--light" disabled={!filteredTickets.length || busy} onClick={downloadCurrent}>Xuất chi tiết đang lọc</button><button className="admin-button admin-button--primary" disabled={!current?.data || busy} onClick={downloadAll}>Xuất tổng hợp cả kỳ</button></div></div>
      {valid && !current && <p role="status">Đang tải báo cáo...</p>}
      {current?.error && <div role="alert"><p className="form-message form-message--error">{current.error}</p>
        <button type="button" className="admin-button admin-button--light" onClick={() => setRetry(n => n + 1)}>Thử lại</button>
      </div>}
    </section>

    {current?.data && <><div className="rv4-kpis">
      <KPI title="Tổng vé hợp lệ" value={totals.total.toLocaleString('vi-VN')} helper={formatPct(pctChange(totals.total, prior.total))} tone="rv4-kpi--blue" />
      <KPI title="Tổng tiền" value={displayMoney(totals.amount)} helper={formatPct(pctChange(totals.amount, prior.amount))} tone="rv4-kpi--gold" />
      <KPI title="Suất thường" value={totals.normal.toLocaleString('vi-VN')} helper={`Đặc biệt: ${totals.special} vé`} />
      <KPI title="Vé mua thêm" value={totals.purchased.toLocaleString('vi-VN')} helper={`${totals.received} vé đang sở hữu có nhận chuyển nhượng`} />
    </div>

    <div className="rv4-visuals">
      <section className="rv4-card"><header className="rv4-card-heading"><div><h3>Suất ăn theo ngày</h3><p>Thứ Hai đến thứ Sáu · Chạm vào cột để lọc chi tiết</p></div>{selectedDay && <button className="rv4-clear" onClick={() => setSelectedDay('')}>Bỏ lọc ngày ×</button>}</header>
        <AutoScrollChart days={days} onSelectDay={focusDay} selectedDate={selectedDay} label="Biểu đồ suất ăn theo ngày, nhấn vào cột để xem chi tiết" />
      </section>
      <section className="rv4-card"><header className="rv4-card-heading"><div><h3>Phân bổ phòng ban</h3><p>Nhấn phòng để xem nhân viên và chi phí tương ứng</p></div>{department !== 'all' && <button className="rv4-clear" onClick={() => setDepartment('all')}>Tất cả ×</button>}</header>
        <div className="rv4-departments">{departments.length ? departments.map(d => <button type="button" key={d.id} className="rv4-department" onClick={() => focusDepartment(d.id)}><div><strong>{d.name}</strong><span>{d.tickets} vé · {displayMoney(d.amount)}</span></div><div className="rv4-track"><span style={{ width: `${Math.max(2, 100 * d.tickets / Math.max(1, totals.total))}%` }} /></div><small>{d.people} nhân viên · {d.special} suất đặc biệt</small></button>) : <p className="rv4-empty">Không có vé phù hợp với bộ lọc.</p>}</div>
        <p className="rv4-transfer-note">{activity} vé được nhận qua giao dịch chuyển nhượng hoàn tất trong kỳ (thống kê giao dịch, không cộng thêm vào tổng vé).</p>
      </section>
    </div>

    <section className="rv4-card rv4-detail" ref={detailRef} aria-label="Báo cáo chi tiết">
      <header className="rv4-card-heading rv4-detail-heading"><div><h3>Chi tiết nhân viên</h3><p>{selectedDay ? `Riêng ngày ${displayDate(selectedDay)}` : `Trong kỳ ${primaryPeriodLabel}`} · {detailRows.length} nhân viên · {totalDetail.total} vé · {displayMoney(totalDetail.amount)}</p></div><div className="rv4-detail-tools">{selectedDay && <button className="rv4-clear" onClick={() => setSelectedDay('')}>Bỏ chọn ngày</button>}<button className="admin-button admin-button--light" disabled={!filteredTickets.length || busy} onClick={downloadCurrent}>Xuất bảng chi tiết</button></div></header>
      <div className="rv4-table-viewport"><table className="rv4-table"><thead><tr><th>Nhân viên</th><th>Phòng ban</th><th>Suất thường</th><th>Suất đặc biệt</th><th>Mua thêm</th><th>Tổng vé</th><th>Tổng tiền</th><th>Chi tiết</th></tr></thead><tbody>
        {visiblePeople.map(row => <tr key={row.person.id}><td><strong>{row.person.name}</strong><small>{row.person.code}</small></td><td>{departmentNames.get(String(row.person.departmentId)) || '—'}</td><td>{row.normal}</td><td>{row.special}</td><td>{row.purchased}</td><td><strong>{row.tickets}</strong></td><td><strong>{displayMoney(row.amount)}</strong></td><td><button className="rv4-link" onClick={() => setEmployeeId(String(row.person.id))}>Xem vé</button></td></tr>)}
        {!visiblePeople.length && <tr><td colSpan={8} className="rv4-empty">Không có nhân viên phù hợp với bộ lọc.</td></tr>}
      </tbody></table></div>
      <footer className="rv4-pagination"><span>Trang {Math.min(page, totalPages)}/{totalPages} · {detailRows.length} nhân viên</span><div><button type="button" disabled={page <= 1} onClick={() => setPage(p => p - 1)}>‹ Trước</button><button type="button" disabled={page >= totalPages} onClick={() => setPage(p => p + 1)}>Sau ›</button></div></footer>
    </section>

    {selectedEmployee && <ReportEmployeeDrawer selectedEmployee={selectedEmployee} employeeId={employeeId}
      employeeTickets={employeeTickets} transactions={transactions} departmentNames={departmentNames}
      employeeName={id => human(state, id)} onClose={() => setEmployeeId('')} />}</>}
  </div>;
}
