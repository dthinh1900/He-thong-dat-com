export default function RegistrationLegend({ onViewCancelled }) {
  return <div className="registration-legend" aria-label="Chú thích trạng thái đăng ký">
    <div className="registration-legend__row">
      <div className="registration-legend__item"><span className="registration-legend__dot registration-legend__dot--registered" aria-hidden="true" /><span>Ngày đã đăng ký ăn</span></div>
      <div className="registration-legend__item"><span className="registration-legend__dot registration-legend__dot--special" aria-hidden="true" /><span>Suất đặc biệt</span></div>
      <button className="registration-legend__item registration-legend__cancelled" type="button" onClick={onViewCancelled} aria-haspopup="dialog">
        <span className="registration-legend__dot registration-legend__dot--cancelled" aria-hidden="true">×</span>
        Xem vé đã hủy
      </button>
    </div>
    <div className="registration-legend__item"><span className="registration-legend__dot" aria-hidden="true" /><span>Ngày chưa đăng ký ăn</span></div>
    <p className="registration-legend__hint">Ngày đổi màu ngay khi chọn; nhấn Lưu đăng ký để xác nhận. Sau hạn bỏ chọn, liên hệ quản trị viên để hủy vé. Dấu × chỉ vé do quản trị viên hủy.</p>
  </div>;
}
