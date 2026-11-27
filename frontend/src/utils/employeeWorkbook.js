import { downloadWorkbook, loadExcel } from './workbook';

const aliases = {
  code: ['mã nhân viên', 'ma nhan vien', 'code'],
  name: ['họ tên', 'ho ten', 'họ và tên', 'name'],
  username: ['tài khoản', 'tai khoan', 'username'],
  departmentName: ['phòng ban', 'phong ban', 'department'],
  email: ['email'],
  phone: ['số điện thoại', 'so dien thoai', 'phone'],
  role: ['vai trò', 'vai tro', 'role'],
  password: ['mật khẩu', 'mat khau', 'password'],
};
const normalized = value => String(value ?? '').trim().toLowerCase();
const requiredFields = ['code', 'name', 'username', 'departmentName'];
const fieldLengths = { code: 50, name: 160, username: 80, email: 160, phone: 30 };

export async function exportEmployeeTemplate(departmentName) {
  const excel = await loadExcel();
  const workbook = new excel.Workbook();
  const sheet = workbook.addWorksheet('Nhan vien');
  sheet.addRow(['Mã nhân viên', 'Họ tên', 'Tài khoản', 'Phòng ban', 'Email', 'Số điện thoại', 'Vai trò', 'Mật khẩu']);
  sheet.addRow(['NV-MAU', 'Nguyễn Văn Mẫu', 'nv-mau', departmentName ?? '', '', '', 'USER', '']);
  sheet.getRow(1).font = { bold: true };
  sheet.columns = [20, 30, 20, 30, 30, 20, 15, 22].map(width => ({ width }));
  sheet.views = [{ state: 'frozen', ySplit: 1 }];
  await downloadWorkbook(workbook, 'mau-import-nhan-vien.xlsx');
}

export async function parseEmployeeFile(file, { departments, employees }) {
  if (file.size > 5 * 1024 * 1024) throw new Error('File Excel không được vượt quá 5 MB.');
  const excel = await loadExcel();
  const workbook = new excel.Workbook();
  await workbook.xlsx.load(await file.arrayBuffer());
  const sheet = workbook.worksheets[0];
  if (!sheet) throw new Error('Không thấy sheet nhân viên.');
  const header = sheet.getRow(1);
  const columns = {};
  header.eachCell((cell, index) => {
    const field = Object.keys(aliases).find(key => aliases[key].includes(normalized(cell.text)));
    if (field && !columns[field]) columns[field] = index;
  });
  if (requiredFields.some(field => !columns[field]) || !columns.password) {
    throw new Error('Excel thiếu cột Mã nhân viên, Họ tên, Tài khoản, Phòng ban hoặc Mật khẩu.');
  }
  const codeSet = new Set(employees.map(employee => normalized(employee.code)));
  const userSet = new Set(employees.map(employee => normalized(employee.username)));
  const departmentByName = new Map(departments.map(item => [normalized(item.name), item]));
  const rows = [];
  sheet.eachRow((row, number) => {
    if (number === 1) return;
    const cells = Object.fromEntries(Object.entries(columns).map(([field, index]) => [field, row.getCell(index).text]));
    if (Object.values(cells).every(value => !value.trim())) return;
    const { departmentName = '', password = '', ...other } = cells;
    const employee = Object.fromEntries(Object.keys(fieldLengths).map(field => [field, other[field]?.trim() || '']));
    const department = departmentByName.get(normalized(departmentName));
    const role = (other.role?.trim() || 'USER').toUpperCase();
    const errors = [];
    if (requiredFields.some(field => !cells[field]?.trim())) errors.push('Thiếu thông tin bắt buộc');
    if (!department?.active) errors.push('Phòng ban chưa hoạt động hoặc không tìm thấy');
    if (codeSet.has(normalized(employee.code))) errors.push('Trùng mã nhân viên');
    if (userSet.has(normalized(employee.username))) errors.push('Trùng tài khoản');
    if (!['USER', 'ADMIN'].includes(role)) errors.push('Vai trò không hợp lệ');
    if (password.length < 8 || !password.trim() || new TextEncoder().encode(password).length > 72) {
      errors.push('Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte');
    }
    if (Object.entries(fieldLengths).some(([field, max]) => employee[field].length > max)) {
      errors.push('Có trường vượt độ dài cho phép');
    }
    if (employee.email && !/^[^\s@]+@[^\s@]+$/.test(employee.email)) errors.push('Email không hợp lệ');
    if (employee.code) codeSet.add(normalized(employee.code));
    if (employee.username) userSet.add(normalized(employee.username));
    rows.push({ row: number, departmentName: departmentName.trim(), error: errors.join('; '),
      employee: { ...employee, email: employee.email || null, phone: employee.phone || null,
        departmentId: department?.id, role, active: true, password },
    });
  });
  if (rows.length > 500) throw new Error('Mỗi lần nhập được tối đa 500 nhân viên.');
  if (!rows.length) throw new Error('File Excel chưa có dòng nhân viên.');
  return rows;
}
