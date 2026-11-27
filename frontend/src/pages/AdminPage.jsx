import { useCallback, useEffect, useState } from 'react';
import { useToast } from '../hooks/useToast';
import CompactNavigation from '../components/CompactNavigation';
import { getAdminOverview } from '../services/adminService';
import AdminOverviewSection from '../features/admin/AdminOverviewSection';
import AdminPeopleSection from '../features/admin/AdminPeopleSection';
import AdminMenuSection from '../features/admin/AdminMenuSection';
import AdminRegistrationsSection from '../features/admin/AdminRegistrationsSection';
import ReportDashboard from '../features/reports/ReportDashboard';
import '../styles/admin.css';


const navItems = [
  ['overview', 'Tổng quan', '▦'],
  ['people', 'Nhân viên & phòng ban', '♙'],
  ['menu', 'Thực đơn & giá vé', '▤'],
  ['registrations', 'Đăng ký & giao dịch', '☷'],
  ['reports', 'Báo cáo & thống kê', '▥'],
];

export default function AdminPage() {
  const { notify } = useToast();
  const [activeSection, setActiveSection] = useState('overview');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const [overview, setOverview] = useState(null);
  const [overviewRefresh, setOverviewRefresh] = useState(0);
  const [reportDay, setReportDay] = useState('');

  const [departments, setDepartments] = useState([]);
  const [ticketTab, setTicketTab] = useState('roster');
  const [navigationRevision, setNavigationRevision] = useState(0);

  const loadOverview = useCallback(async () => {
    setLoading(true);
    try {
      setOverview(await getAdminOverview());
    } catch (error) {
      notify(error.message || 'Không tải được số liệu tổng quan.', 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    if (activeSection === 'overview') loadOverview();
  }, [activeSection, loadOverview]);

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-sidebar__brand">
          <span className="admin-sidebar__mark">VP</span>
          <span><b>QUẢN TRỊ</b><small>HỆ THỐNG ĐẶT CƠM</small></span>
        </div>
        <div className="admin-sidebar__label">MENU QUẢN TRỊ</div>
        <CompactNavigation label="Menu quản trị" className="admin-navigation" itemsClassName="admin-nav"
          currentLabel={navItems.find(([id]) => id === activeSection)?.[1]}>
          {close => navItems.map(([id, label, icon]) => (
            <button key={id} type="button" aria-current={activeSection === id ? 'page' : undefined}
              className={`admin-nav__item${activeSection === id ? ' is-active' : ''}`} onClick={() => {
              setActiveSection(id);
              setReportDay('');
              setNavigationRevision(current => current + 1);
              close();
            }}>
              <span aria-hidden="true">{icon}</span>{label}
            </button>
          ))}
        </CompactNavigation>
        <div className="admin-sidebar__bottom">Dữ liệu từ hệ thống</div>
      </aside>

      <section className="admin-main">
        <header className="admin-topbar">
          <div><span className="admin-topbar__eyebrow">HỆ THỐNG ĐẶT CƠM</span><h1>{navItems.find(([id]) => id === activeSection)?.[1]}</h1></div>
          <span className="admin-topbar__badge">ADMIN</span>
        </header>

        <div className="admin-content">
          {loading && <div className="admin-loading-note">Đang tải dữ liệu...</div>}

          {activeSection === 'overview' && <AdminOverviewSection overview={overview} loading={loading}
            refreshKey={overviewRefresh} onRefresh={() => { loadOverview(); setOverviewRefresh(n => n + 1); }}
            onSelectReportDay={date => { setReportDay(date); setActiveSection('reports'); }} />}
          <AdminPeopleSection active={activeSection === 'people'} navigationRevision={navigationRevision}
            departments={departments} setDepartments={setDepartments} loading={loading} setLoading={setLoading}
            saving={saving} setSaving={setSaving} />
          <AdminMenuSection active={activeSection === 'menu'} loading={loading} setLoading={setLoading}
            saving={saving} setSaving={setSaving} />
          <AdminRegistrationsSection active={activeSection === 'registrations'} ticketTab={ticketTab} setTicketTab={setTicketTab}
            departments={departments} setDepartments={setDepartments} loading={loading} setLoading={setLoading}
            saving={saving} setSaving={setSaving} onRegistrationChanged={loadOverview} />

          {activeSection === 'reports' && <ReportDashboard initialDay={reportDay} />}
        </div>
      </section>
    </div>
  );
}
