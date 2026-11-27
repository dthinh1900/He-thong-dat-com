import { useState } from 'react';
import { useToast } from '../../hooks/useToast';
import { importEmployees } from '../../services/adminService';
import { exportEmployeeTemplate, parseEmployeeFile } from '../../utils/employeeWorkbook';

export default function AdminBulkImport({ departments, employees, onImported }) {
  const { notify } = useToast();
  const [rows, setRows] = useState([]);
  const [busy, setBusy] = useState(false);
  const errors = rows.filter(row => row.error).length;
  const canImport = rows.length > 0 && errors === 0;

  async function choose(event) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setBusy(true);
    setRows([]);
    try { setRows(await parseEmployeeFile(file, { departments, employees })); }
    catch (error) { notify(error.message || 'Không đọc được file Excel.', 'error'); }
    finally { setBusy(false); }
  }

  async function submit() {
    if (!canImport) return;
    setBusy(true);
    try {
      const created = await importEmployees(rows.map(({ row, employee }) => ({ row, employee })));
      setRows([]);
      notify(`Đã thêm ${created.length} nhân viên.`, 'success');
      await onImported();
    } catch (error) { notify(error.message || 'Không nhập được danh sách nhân viên.', 'error'); }
    finally { setBusy(false); }
  }

  return <details className="preview-import-details">
    <summary>⇧ Import danh sách nhân viên từ Excel</summary>
    <div className="preview-import-body">
      <p className="preview-note">Điền mật khẩu ban đầu cho từng tài khoản, rồi kiểm tra danh sách trước khi nhập.</p>
      <div className="preview-actions">
        <button type="button" className="admin-button admin-button--light" disabled={busy} onClick={() =>
          exportEmployeeTemplate(departments.find(d => d.active)?.name).catch(error => notify(error.message, 'error'))}>
          Tải file mẫu
        </button>
        <label className="preview-upload">Chọn file XLSX<input type="file" accept=".xlsx" disabled={busy} onChange={choose} /></label>
      </div>
      {rows.length > 0 && <>
        <p>{rows.length} dòng · {errors} lỗi</p>
        <div className="preview-table-wrap">
          <table className="preview-table">
            <thead><tr><th>Dòng</th><th>Mã NV</th><th>Họ tên</th><th>Tài khoản</th><th>Phòng ban</th><th>Kiểm tra</th></tr></thead>
            <tbody>{rows.map(({ row, employee, departmentName, error }) => <tr key={row}>
              <td>{row}</td><td>{employee.code}</td><td>{employee.name}</td><td>{employee.username}</td>
              <td>{departmentName}</td><td>{error || 'Hợp lệ'}</td>
            </tr>)}</tbody>
          </table>
        </div>
        <button type="button" className="admin-button admin-button--primary" disabled={!canImport || busy} onClick={submit}>
          {busy ? 'Đang nhập...' : `Nhập ${rows.length} nhân viên`}
        </button>
      </>}
    </div>
  </details>;
}
