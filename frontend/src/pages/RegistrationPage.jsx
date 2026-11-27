import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import CompactNavigation from '../components/CompactNavigation';
import MonthCalendar from '../components/MonthCalendar';
import RegistrationLegend from '../components/RegistrationLegend';
import RegistrationSummary from '../components/RegistrationSummary';
import { useCurrentTime } from '../hooks/useCurrentTime';
import { useRegistration } from '../hooks/useRegistration';
import { useToast } from '../hooks/useToast';
import { formatVietnameseDate, shiftMonth } from '../utils/date';
import { todayKey } from '../utils/mealDates';
import { MenuPopup } from '../features/registration/WeekMenuPopup';
import DepartmentLookupPanel from '../features/registration/DepartmentLookupPanel';
import FridaySpecialPanel from '../features/registration/FridaySpecialPanel';
import CancelledTicketsDialog from '../features/registration/CancelledTicketsDialog';

export default function RegistrationPage() {
  const now = useCurrentTime();
  const { notify } = useToast();
  const [monthKey, setMonthKey] = useState(() => todayKey().slice(0, 7));
  const [rosterRevision, setRosterRevision] = useState(0);
  const [cancelledView, setCancelledView] = useState(null);
  const [activeFridayDate, setActiveFridayDate] = useState('');
  const [params, setParams] = useSearchParams();
  const menuOpen = params.get('menu') === '1';
  const menuDate = params.get('menuDate');
  const currentTab = params.get('tab') === 'department' ? 'department' : 'registration';
  const { selectedDays, selectedSet, selectedTypes, dayMap, toggleDate, chooseType,
    toggleAllWorkdays, allWorkdaysSelected, editableCount, totalAmount, totalTickets,
    priceLabel, cutoffTime, cancelDeadline, hasChanges, loading, saving, loadError, save, discardChanges } = useRegistration(monthKey, now);

  const cancelledDays = Object.values(dayMap).filter(day => day.cancelledTickets?.length);

  function inform(result) { notify(result.message, result.type || (result.ok ? 'success' : 'error')); }
  function changeTab(tab) { const next = new URLSearchParams(params); if (tab === 'registration') next.delete('tab'); else next.set('tab', tab); setParams(next, {replace: true}); }
  function setMenuOpen(open, date) {
    const next = new URLSearchParams(params);
    if (open) next.set('menu', '1');
    else next.delete('menu');
    if (open && date) next.set('menuDate', date);
    else next.delete('menuDate');
    setParams(next, { replace: true });
  }
  function chooseDay(date) {
    if (!dayMap[date]?.editable) return;
    const weekday = new Date(`${date}T12:00:00Z`).getUTCDay();
    const result = toggleDate(date);

    // Clicking a Friday opens its type panel even when the click is
    // removing an already-saved ticket. Choosing a type in the panel
    // can add the ticket back; leaving it untouched keeps it cancelled.
    setActiveFridayDate(result.ok && weekday === 5 ? date : '');
    inform(result);
  }
  function selectAllDays() { setActiveFridayDate(''); inform(toggleAllWorkdays()); }
  function chooseFriday(date, type) { inform(chooseType(date, type)); }
  async function saveRegistration() {
    const result = await save();
    inform(result);
    if (result.ok) setRosterRevision(value => value + 1);
  }

  return <div className="registration-page registration-page--workspace">
    <CompactNavigation label="Chức năng đăng ký" className="registration-navigation workspace-navigation"
      currentLabel={currentTab === 'department' ? 'Tra cứu phòng' : 'Đăng ký cơm'} itemsClassName="registration-mobile-tabs workspace-tabs">
      {close => [
        ['registration', 'Đăng ký cơm'], ['department', 'Tra cứu phòng'],
      ].map(([tab, label]) => (
        <button key={tab} type="button" aria-pressed={currentTab === tab}
          className={currentTab === tab ? 'is-active' : ''} onClick={() => { changeTab(tab); close(); }}>{label}</button>
      ))}
    </CompactNavigation>
    <div className="registration-workspace">
      <div className={`registration-department-area${currentTab === 'department' ? ' is-visible' : ''}`}><DepartmentLookupPanel revision={rosterRevision} /></div>
      <div className={`registration-main-area${currentTab === 'registration' ? ' is-visible' : ''}`}>
        <h1 className="registration-page__title">Hệ thống đăng ký cơm trưa cho nhân viên</h1>
        <section className="registration-panel" aria-labelledby="registration-date">
          <div className="registration-panel__intro">
            <h2 id="registration-date">Ngày {formatVietnameseDate(now)}</h2>
            {cutoffTime && <p>
              Đăng ký trước {cutoffTime.slice(0, 5)}{cancelDeadline && <> và bỏ chọn trước {cancelDeadline.slice(0, 5)}</>} ngày liền trước ngày ăn (giờ Việt Nam).
              {' '}Không phục vụ thứ Bảy, Chủ nhật.
            </p>}
          </div>
          <RegistrationLegend onViewCancelled={() => setCancelledView({})} />
          {loadError && <p className="form-message form-message--error" role="alert">{loadError}</p>}
          {loading && <p className="registration-panel__loading">Đang tải lịch đăng ký...</p>}
          <div className="registration-panel__body">
            <MonthCalendar monthKey={monthKey} selectedSet={selectedSet} selectedTypes={selectedTypes} dayMap={dayMap} now={now} saving={saving} onToggleDate={chooseDay} onViewCancelled={date => setCancelledView({ date })} onPrevMonth={() => setMonthKey(key => shiftMonth(key, -1))} onNextMonth={() => setMonthKey(key => shiftMonth(key, 1))} />
            <div className="registration-side-column">
              <RegistrationSummary selectedCount={selectedDays.length} totalTickets={totalTickets} totalAmount={totalAmount} priceLabel={priceLabel} onSelectAll={selectAllDays} allSelected={allWorkdaysSelected} editableCount={saving ? 0 : editableCount} draft={hasChanges} onMenuClick={() => setMenuOpen(true)} />
            </div>
          </div>
          <div className="registration-friday-note">
            {cutoffTime && activeFridayDate && <FridaySpecialPanel activeDate={activeFridayDate} now={now} cutoffTime={cutoffTime} saving={saving} selectedSet={selectedSet} selectedTypes={selectedTypes} dayMap={dayMap} onChoose={chooseFriday} onViewMenu={date => setMenuOpen(true, date)} />}
          </div>
          <div className="registration-panel__footer">{hasChanges && <div className="registration-panel__unsaved"><span role="status">Có thay đổi chưa lưu</span><button className="preview-link" type="button" onClick={discardChanges} disabled={saving}>Hoàn tác</button></div>}<button className="button button--save" type="button" disabled={loading || saving || Boolean(loadError) || !hasChanges} onClick={saveRegistration}>{saving ? 'Đang lưu...' : 'Lưu đăng ký'}</button></div>
        </section>
      </div>
    </div>
    {cancelledView && <CancelledTicketsDialog days={cancelledDays} date={cancelledView.date}
      onClose={() => setCancelledView(null)} onToggleDate={chooseDay} selected={selectedSet.has(cancelledView.date)} />}
    {menuOpen && <MenuPopup key={menuDate || 'current'} weekDate={menuDate} onClose={() => setMenuOpen(false)} />}
  </div>;
}
