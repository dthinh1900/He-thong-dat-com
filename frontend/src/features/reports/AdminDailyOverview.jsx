import { useEffect, useState } from 'react';
import { todayKey, displayMoney } from '../../utils/mealDates';
import { getMonthlyReport } from '../../services/adminService';
import AutoScrollChart from './AutoScrollChart';
import './report.css';

export default function AdminDailyOverview({ refreshKey, onSelectDay }) {
  const [month, setMonth] = useState(() => todayKey().slice(0, 7));
  const [result, setResult] = useState(null);
  useEffect(() => {
    if (!month) return undefined;
    let active = true;
    const [year, monthNumber] = month.split('-').map(Number);
    getMonthlyReport(monthNumber, year)
      .then(report => { if (active) setResult({ month, refreshKey, report }); })
      .catch(error => { if (active) setResult({ month, refreshKey, error: error.message }); });
    return () => { active = false; };
  }, [month, refreshKey]);
  const current = result?.month === month && result.refreshKey === refreshKey ? result : null;
  return <section className="preview-overview-chart" aria-label="Biểu đồ tổng số suất theo ngày">
    <div className="preview-chart-heading">
      <div><h2>Số suất ăn theo từng ngày</h2><p>Thứ Hai đến thứ Sáu · Kéo ngang để xem thêm</p></div>
      <label>Tháng <input type="month" value={month} onChange={event => setMonth(event.target.value)} /></label>
    </div>
    {!month ? <p>Chọn tháng để xem biểu đồ.</p>
      : current?.error ? <p className="form-message form-message--error" role="alert">{current.error}</p>
      : current?.report ? <>
        <div className="preview-chart-metrics"><strong>{current.report.totalTickets} suất</strong><span>{displayMoney(current.report.totalAmount)}</span></div>
        <AutoScrollChart days={current.report.days} onSelectDay={onSelectDay} label="Tổng suất ăn theo ngày" />
      </> : <p role="status">Đang tải số suất ăn...</p>}
  </section>;
}
