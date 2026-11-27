import { AdminEmployeeHistory } from '../tickets/AdminTicketTransactions';

export default function AdminEmployeeDialog({ employee, draft, setDraft, departments, saving,
  onClose, onCancelEdit, onSave, onChangeActive, onRetire }) {
  return (
    <div className="admin-modal-backdrop admin-employee-modal-backdrop" onClick={onClose}>
      <div className="admin-modal admin-employee-modal" onClick={(event) => event.stopPropagation()}>
        <div className="admin-modal__heading">
          <h3>{draft ? (employee.id ? 'Sửa thông tin nhân viên' : 'Thêm nhân viên') : 'Thông tin nhân viên'}</h3>
          <button type="button" aria-label="Đóng" onClick={onClose}>×</button>
        </div>
        {!draft ? <>
          <div className="admin-profile-avatar">{employee.name?.slice(0, 1) || 'N'}</div>
          <h2 className="admin-employee-modal__name">{employee.name || 'Nhân viên mới'}</h2>
          <p className="admin-drawer__muted">{employee.code || 'Chưa có mã'} · {employee.username || 'Chưa có tài khoản'}</p>
          <dl className="admin-employee-detail-grid">
            <div><dt>Mã nhân viên</dt><dd>{employee.code || '—'}</dd></div>
            <div><dt>Họ và tên</dt><dd>{employee.name || '—'}</dd></div>
            <div><dt>Tên đăng nhập</dt><dd>{employee.username || '—'}</dd></div>
            <div><dt>Phòng ban</dt><dd>{departments.find((item) => item.id === employee.departmentId)?.name || '—'}</dd></div>
            <div><dt>Email</dt><dd>{employee.email || '—'}</dd></div>
            <div><dt>Số điện thoại</dt><dd>{employee.phone || '—'}</dd></div>
            <div><dt>Vai trò</dt><dd>{employee.role || 'USER'}</dd></div>
            <div><dt>Trạng thái</dt><dd><span className={`admin-pill ${employee.active ? 'is-active' : 'is-locked'}`}>{employee.active ? 'Đang hoạt động' : 'Đã khóa'}</span></dd></div>
          </dl>
           <AdminEmployeeHistory employeeId={employee.id} />
          <div className="admin-modal__actions admin-employee-modal__actions">
            <button type="button" className="admin-button admin-button--primary" onClick={() => setDraft({ ...employee, password: '' })}>Sửa thông tin</button>
            <button type="button" className="admin-button admin-button--light" disabled={saving || employee.id == null} onClick={onChangeActive}>{employee.active ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}</button>
            {employee.active && <button type="button" className="admin-button admin-button--danger" disabled={saving} onClick={onRetire}>Ngừng hoạt động</button>}
          </div>
          <p className="admin-modal-help">Backend không xóa cứng nhân viên để bảo toàn vé ăn và lịch sử giao dịch. “Ngừng hoạt động” sẽ chuyển tài khoản sang trạng thái inactive.</p>
        </> : <>
          <div className="admin-form-grid admin-employee-edit-grid">
            {[
              ['code', 'Mã nhân viên'], ['name', 'Họ và tên'], ['username', 'Tên đăng nhập'],
              ['email', 'Email'], ['phone', 'Số điện thoại'],
            ].map(([key, label]) => <label key={key}>{label}
              <input value={draft[key] || ''} onChange={(event) => setDraft((item) => ({ ...item, [key]: event.target.value }))} />
            </label>)}
            <label>Phòng ban
              <select value={draft.departmentId ?? ''} onChange={(event) => setDraft((item) => ({ ...item, departmentId: Number(event.target.value) }))}>
                {departments.filter((department) => department.active || department.id === draft.departmentId).map((department) => <option key={department.id} value={department.id}>{department.name}{department.active ? '' : ' (đã ngừng hoạt động)'}</option>)}
              </select>
            </label>
            <label>Vai trò
              <select value={draft.role} onChange={(event) => setDraft((item) => ({ ...item, role: event.target.value }))}>
                <option value="USER">USER</option><option value="ADMIN">ADMIN</option>
              </select>
            </label>
            <label>Mật khẩu {draft.id ? '(để trống nếu không đổi)' : '(bắt buộc)'}
              <input type="password" autoComplete="new-password" value={draft.password || ''} onChange={(event) => setDraft((item) => ({ ...item, password: event.target.value }))} />
            </label>
            <label className="admin-checkbox-field"><input type="checkbox" checked={Boolean(draft.active)} onChange={(event) => setDraft((item) => ({ ...item, active: event.target.checked }))} /> Tài khoản đang hoạt động</label>
          </div>
          <div className="admin-modal__actions">
            <button type="button" className="admin-button admin-button--light" onClick={onCancelEdit} disabled={saving}>Hủy</button>
            <button type="button" className="admin-button admin-button--primary" onClick={onSave} disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu thông tin'}</button>
          </div>
        </>}
      </div>
    </div>
  );
}
