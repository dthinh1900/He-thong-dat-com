import { downloadWorkbook, loadExcel } from '../../utils/workbook';

export async function writeRegistrationWorkbook({ registrationRows, departments, onlyRegistered, departmentFilter, registrationDate }) {
  const ExcelJS = await loadExcel();
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'Hệ thống đặt cơm';
  workbook.created = new Date();
  const sheet = workbook.addWorksheet('Dang ky va giao dich', {
    views: [{ state: 'frozen', ySplit: 1 }],
    autoFilter: 'A1:G1',
  });
  sheet.columns = [
    { header: 'STT', key: 'index', width: 8 },
    { header: 'Ngày đăng ký', key: 'date', width: 16 },
    { header: 'Phòng ban', key: 'department', width: 26 },
    { header: 'Mã nhân viên', key: 'code', width: 18 },
    { header: 'Tên nhân viên', key: 'name', width: 30 },
    { header: 'Trạng thái vé ăn', key: 'status', width: 20 },
    { header: 'Ghi chú', key: 'note', width: 36 },
  ];
  const departmentNames = new Map(departments.map((item) => [String(item.id), item.name]));
  const rows = registrationRows.filter((row) => {
    if (onlyRegistered && !row.registered) return false;
    if (departmentFilter !== 'all' && String(row.departmentId) !== String(departmentFilter)) return false;
    return true;
  }).sort((a, b) => String(a.name ?? '').localeCompare(String(b.name ?? ''), 'vi'));
  rows.forEach((row, index) => sheet.addRow({
    index: index + 1,
    date: registrationDate,
    department: departmentNames.get(String(row.departmentId)) || `Phòng ban #${row.departmentId}`,
    code: row.code || '',
    name: row.name || '',
    status: row.registered ? 'Đã đăng ký' : 'Chưa đăng ký',
    note: row.status || row.lockReason || '',
  }));
  const header = sheet.getRow(1);
  header.height = 24;
  header.eachCell((cell) => {
    cell.font = { bold: true, color: { argb: 'FFFFFFFF' } };
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF176DA5' } };
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
    cell.border = { bottom: { style: 'thin', color: { argb: 'FFDCE4EC' } } };
  });
  sheet.eachRow((row, rowNumber) => {
    if (rowNumber > 1) {
      row.eachCell((cell) => {
        cell.alignment = { vertical: 'middle', wrapText: true };
        cell.border = { bottom: { style: 'hair', color: { argb: 'FFE5EAF0' } } };
      });
    }
  });
  const filename = `dang-ky-va-giao-dich-${registrationDate}.xlsx`;
  await downloadWorkbook(workbook, filename);
  return rows.length;
}
