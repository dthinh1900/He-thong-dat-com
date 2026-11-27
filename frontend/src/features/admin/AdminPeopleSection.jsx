import { sortVietnamese } from './adminFormatting';
import AdminBulkImport from './AdminBulkImport';
import AdminEmployeeDialog from './AdminEmployeeDialog';
import AdminDepartmentDialog from './AdminDepartmentDialog';

import { useAdminPeople } from './useAdminPeople';

export default function AdminPeopleSection({ active, navigationRevision, departments, setDepartments, loading, setLoading, saving, setSaving }) {
  const {
    employees, selectedEmployee, setSelectedEmployee, employeeDraft, setEmployeeDraft, departmentDraft, setDepartmentDraft,
    employeeSearch, setEmployeeSearch, appliedEmployeeSearch, setAppliedEmployeeSearch, employeeDepartmentFilter, setEmployeeDepartmentFilter, filteredEmployees,
    loadPeople, startAddEmployee, cancelEmployeeEdit, saveEmployee, changeEmployeeActive, retireSelectedEmployee, saveDepartment, openEmployee,
  } = useAdminPeople({ active, navigationRevision, departments, setDepartments, setLoading, setSaving });

  if (!active) return null;

  return (
    <>
    <div className="admin-section-heading">
      <div><h2>Phòng ban</h2><p>Quản lý phòng ban và danh sách nhân viên theo từng phòng.</p></div>
      <button className="admin-button admin-button--primary" onClick={() => setDepartmentDraft({ name: '', description: '', active: true })}>＋ Thêm phòng ban</button>
    </div>
    <div className="admin-department-list">
      {departments.map((department) => (
        <div className="admin-department-card" key={department.id}>
          <div>
            <b>{department.name}</b>
            <small>{department.description || 'Chưa có mô tả'} · {department.employeeCount ?? 0} nhân viên · {department.active ? 'Đang hoạt động' : 'Đã ngừng hoạt động'}</small>
          </div>
          <button className="admin-button admin-button--light" onClick={() => setDepartmentDraft({ ...department })}>Sửa</button>
        </div>
      ))}
      {!loading && departments.length === 0 && <div className="admin-empty">Chưa có phòng ban trong hệ thống.</div>}
    </div>

    <AdminBulkImport departments={departments} employees={employees} onImported={loadPeople} />
    <div className="admin-section-heading admin-section-heading--spaced">
      <div><h2>Danh sách nhân viên</h2><p>Danh sách được sắp xếp theo tên A–Z trong từng phòng ban.</p></div>
      <button className="admin-button admin-button--primary" onClick={startAddEmployee}>＋ Thêm nhân viên</button>
    </div>
    <div className="admin-filter-row admin-search-row">
      <label>Tìm nhân viên<input value={employeeSearch} onChange={(event) => setEmployeeSearch(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') setAppliedEmployeeSearch(employeeSearch); }} placeholder="Tên, mã hoặc tài khoản" /></label>
      <button className="admin-button admin-button--light" onClick={() => setAppliedEmployeeSearch(employeeSearch)}>Tìm kiếm</button>
      {appliedEmployeeSearch && <button className="admin-text-button" onClick={() => { setEmployeeSearch(''); setAppliedEmployeeSearch(''); }}>Xóa lọc</button>}
      <label>Phòng ban<select value={employeeDepartmentFilter} onChange={e => setEmployeeDepartmentFilter(e.target.value)}><option value="all">Tất cả phòng</option>{departments.map(d => <option key={d.id} value={String(d.id)}>{d.name}</option>)}</select></label>
    </div>
    {departments.map((department) => {
      const rows = sortVietnamese(filteredEmployees.filter((employee) => employee.departmentId === department.id), 'name');
      if (employeeDepartmentFilter !== 'all' && String(department.id) !== employeeDepartmentFilter) return null;
      if (!rows.length && appliedEmployeeSearch) return null;
      return (
        <section className="admin-table-card" key={department.id}>
          <h3>{department.name}{!department.active ? ' · Đã ngừng hoạt động' : ''}</h3>
          <div className="admin-table-wrap"><table className="admin-table admin-table--employees">
            <thead><tr><th>STT</th><th>Mã nhân viên</th><th>Họ và tên</th><th>Tài khoản</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
            <tbody>{rows.map((employee, index) => (
              <tr key={employee.id}>
                <td>{index + 1}</td><td>{employee.code}</td><td><b>{employee.name}</b></td><td>{employee.username}</td>
                <td><span className={`admin-pill ${employee.active ? 'is-active' : 'is-locked'}`}>{employee.active ? 'Hoạt động' : 'Đã khóa'}</span></td>
                <td><button className="admin-text-button" onClick={() => openEmployee(employee)}>Chi tiết</button></td>
              </tr>
            ))}</tbody>
          </table></div>
          {!rows.length && <div className="admin-empty">Phòng ban chưa có nhân viên phù hợp.</div>}
        </section>
      );
    })}
    {!loading && !departments.length && <div className="admin-empty">Hãy tạo phòng ban trước khi thêm nhân viên.</div>}
      {selectedEmployee && <AdminEmployeeDialog employee={selectedEmployee} draft={employeeDraft}
        setDraft={setEmployeeDraft} departments={departments} saving={saving}
        onClose={() => { setEmployeeDraft(null); setSelectedEmployee(null); }} onCancelEdit={cancelEmployeeEdit}
        onSave={saveEmployee} onChangeActive={changeEmployeeActive} onRetire={retireSelectedEmployee} />}
      {departmentDraft && <AdminDepartmentDialog draft={departmentDraft} setDraft={setDepartmentDraft}
        saving={saving} onClose={() => setDepartmentDraft(null)} onSave={saveDepartment} />}
    </>
  );
}
