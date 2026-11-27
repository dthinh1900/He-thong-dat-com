import { useCallback, useState } from 'react';
import RequestFeedback from '../../components/RequestFeedback';
import { useRequestData } from '../../hooks/useRequestData';
import { getDepartmentRoster, listDepartmentOptions } from '../../services/ticketService';
import { todayKey, displayDate } from '../../utils/mealDates';
import { ticketTypeLabel } from '../tickets/TicketDisplay';

/** Read-only department roster for the employee registration workspace. */
export default function DepartmentLookupPanel({ revision }) {
  const [date, setDate] = useState(todayKey);
  const [department, setDepartment] = useState('all');
  const [showList, setShowList] = useState(true);
  const departments = useRequestData(listDepartmentOptions);
  const loadRoster = useCallback(() => getDepartmentRoster(date, department), [date, department, revision]);
  const roster = useRequestData(date ? loadRoster : null);
  const rows = roster.data?.rows ?? [];
  const departmentName = department === 'all'
    ? 'Tất cả phòng ban'
    : departments.data?.find(item => String(item.id) === department)?.name || 'Phòng ban';

  return <aside className="registration-lookup-card registration-lookup-card--modern" aria-label="Tra cứu suất ăn phòng ban">
    <div className="registration-lookup-card__heading">
      <span className="registration-lookup-card__icon" aria-hidden="true">♙</span>
      <div><h2>Tra cứu phòng ban</h2><p>Danh sách nhân viên đã đăng ký ăn · Chỉ xem</p></div>
    </div>
    <div className="registration-lookup-card__form registration-lookup-card__form--modern">
      <label>Ngày tra cứu<input type="date" value={date} onChange={e => setDate(e.target.value)} /></label>
      <label>Phòng ban<select value={department} onChange={e => setDepartment(e.target.value)} disabled={departments.loading || Boolean(departments.error)}><option value="all">Tất cả phòng ban</option>{departments.data?.map(d => <option value={d.id} key={d.id}>{d.name}</option>)}</select></label>
    </div>
    <RequestFeedback request={departments} loadingText="Đang tải phòng ban..." />
    <RequestFeedback request={roster} loadingText="Đang tải danh sách..." />
    <div className="registration-lookup-card__overview">
      <div className="registration-lookup-card__count registration-lookup-card__count--modern"><span>Đã đăng ký</span><strong>{roster.data?.registeredEmployees ?? '—'}</strong><small>nhân viên{roster.data && ` · ${roster.data.totalTickets} suất`}</small></div>
      <div className="registration-lookup-card__scope"><small>PHẠM VI TRA CỨU</small><b>{departmentName}</b><span>{displayDate(date)}</span></div>
    </div>
    <div className="registration-lookup-card__table-heading">
      <div><b>Danh sách đăng ký</b>{roster.data && <span>{rows.length} kết quả</span>}</div>
      <button type="button" className="registration-lookup-card__toggle registration-lookup-card__toggle--modern" aria-expanded={showList} onClick={() => setShowList(v => !v)}>{showList ? 'Thu gọn' : 'Mở danh sách'} <span aria-hidden="true">{showList ? '⌃' : '⌄'}</span></button>
    </div>
    {showList && roster.data && <div className="registration-lookup-card__table-wrap" role="region" aria-label="Nhân viên đã đăng ký" tabIndex={0}>
      <table className="registration-lookup-table">
        <thead><tr><th>STT</th><th>Nhân viên</th><th>Phòng ban</th><th>Suất ăn</th></tr></thead>
        <tbody>{rows.map((person, index) => (
          <tr key={`${person.userId}-${person.ticketType}`}>
            <td>{index + 1}</td>
            <td><span className="registration-lookup-table__person">
              <span className="registration-lookup-table__avatar" aria-hidden="true">{person.name?.trim()?.charAt(0)?.toUpperCase() || 'N'}</span>
              <b>{person.name}</b>
            </span></td>
            <td>{person.departmentName}</td>
            <td><span className="registration-lookup-table__status"><i /> {person.quantity} · {ticketTypeLabel(person.ticketType)}</span></td>
          </tr>
        ))}</tbody>
      </table>
      {!rows.length && <div className="registration-lookup-card__empty"><span aria-hidden="true">⌕</span><b>Chưa có nhân viên đăng ký</b><p>Thử chọn ngày hoặc phòng ban khác để tra cứu.</p></div>}
    </div>}
  </aside>;
}
