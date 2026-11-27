const XLSX = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

export async function loadExcel() {
  const module = await import('exceljs/dist/exceljs.min.js');
  return module.default?.Workbook ? module.default : (globalThis.ExcelJS?.Workbook ? globalThis.ExcelJS : module);
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 3000);
}

export async function downloadWorkbook(workbook, filename) {
  const contents = await workbook.xlsx.writeBuffer();
  downloadBlob(new Blob([contents], { type: XLSX }), filename);
}
