import { todayKey } from '../../utils/mealDates';
import AdminDailyOverview from '../reports/AdminDailyOverview';
import { dateLabel } from './adminFormatting';

export default function AdminOverviewSection({ overview, loading, refreshKey, onRefresh, onSelectReportDay }) {
  return (
    <>
    <div className="admin-welcome">
      <div><h2>Tổng quan đăng ký ăn</h2><p>Theo dõi nhanh tình hình đăng ký suất ăn của nhân viên.</p></div>
      <span>{dateLabel(overview?.today || todayKey())}</span>
    </div>
    <div className="admin-highlight-grid">
      <article className="admin-highlight admin-highlight--today">
        <span>ĐĂNG KÝ ĂN HÔM NAY</span><strong>{overview?.registeredToday ?? '—'}</strong><small>Nhân viên có vé ăn hợp lệ</small><i>HÔM NAY</i>
      </article>
      <article className="admin-highlight admin-highlight--tomorrow">
        <span>ĐĂNG KÝ ĂN NGÀY MAI</span><strong>{overview?.registeredTomorrow ?? '—'}</strong><small>Nhân viên có vé ăn hợp lệ</small><i>NGÀY MAI</i>
      </article>
    </div>
    <div className="admin-stat-grid">
      <article className="admin-stat-card"><span className="admin-stat-card__icon">♙</span><div><small>TỔNG SỐ NHÂN VIÊN</small><strong>{overview?.employeeCount ?? '—'}</strong></div></article>
      <article className="admin-stat-card"><span className="admin-stat-card__icon">▤</span><div><small>SỐ PHÒNG BAN</small><strong>{overview?.departmentCount ?? '—'}</strong></div></article>
    </div>
    <div className="admin-export-row"><span>Số liệu chỉ tính vé hợp lệ.</span><button className="admin-button admin-button--light" disabled={loading} onClick={onRefresh}>↻ Làm mới</button></div>
    <AdminDailyOverview refreshKey={refreshKey} onSelectDay={onSelectReportDay} />
    </>
  );
}
