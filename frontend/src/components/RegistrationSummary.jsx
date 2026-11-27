import { formatMoney } from '../utils/format';
export default function RegistrationSummary({selectedCount,totalTickets,totalAmount,priceLabel,draft,onSelectAll,allSelected,editableCount,onMenuClick}){
 return <aside className="registration-summary" aria-label="Thống kê đăng ký"><dl className="registration-summary__details">
 <div><dt>Giá tiền:</dt><dd>{priceLabel}</dd></div><div><dt>Tổng ngày ăn:</dt><dd>{selectedCount} ngày</dd></div>
 {totalTickets!==undefined&&totalTickets!==selectedCount&&<div><dt>Tổng vé hợp lệ:</dt><dd>{totalTickets} vé (gồm vé phát sinh)</dd></div>}
 <div><dt>{draft ? 'Tiền dự kiến:' : 'Tổng tiền:'}</dt><dd>{formatMoney(totalAmount)}</dd></div></dl>
 <div className="registration-summary__actions"><button type="button" className="button button--cyan" onClick={onSelectAll} disabled={!editableCount}>{allSelected?'Bỏ chọn tất cả':'Chọn tất cả'}</button><button type="button" className="button button--cyan" onClick={onMenuClick}>Xem thực đơn &gt;&gt;</button></div>
 </aside>;
}
