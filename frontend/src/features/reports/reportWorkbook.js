import { downloadWorkbook, loadExcel } from '../../utils/workbook';

export const sourceName = t => t.transferId ? 'Nhận chuyển nhượng' : t.purchaseId ? 'Mua thêm đã duyệt' : 'Đăng ký tháng';

export async function exportVisibleDetail(tickets, from, to) {
  const api = await loadExcel();
  const book = new api.Workbook();
  const sheet = book.addWorksheet('Chi tiet ve');
  sheet.columns = [
    { header: 'Mã vé', key: 'ticketCode', width: 30 },
    { header: 'Ngày ăn', key: 'date', width: 17 }, { header: 'Mã nhân viên', key: 'code', width: 19 },
    { header: 'Họ và tên', key: 'name', width: 32 }, { header: 'Phòng ban', key: 'department', width: 29 },
    { header: 'Loại suất', key: 'type', width: 17 }, { header: 'Nguồn vé', key: 'source', width: 25 },
    { header: 'Số tiền (VNĐ)', key: 'amount', width: 19 },
  ];
  sheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
  sheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF175587' } };
  sheet.views = [{ state: 'frozen', ySplit: 1 }];
  sheet.autoFilter = { from: 'A1', to: 'H1' };
  for (const t of tickets) {
    sheet.addRow({ ticketCode: t.code ?? t.id, date: t.date, code: t.owner.code, name: t.owner.name,
      department: t.departmentName, type: t.type === 'SPECIAL' ? 'Đặc biệt' : 'Thường',
      source: sourceName(t), amount: t.amount });
  }
  await downloadWorkbook(book, `chi-tiet-ve-${from}-${to}.xlsx`);
}
