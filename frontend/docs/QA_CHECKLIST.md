# Checklist nghiệm thu Frontend v2

> Kiểm thử local trước khi push hoặc deploy Vercel. Chỉ trạng thái được người dùng xác nhận mới được coi là ĐẠT.

- [ ] Đăng nhập bằng tài khoản Admin và nhân viên kiểm thử, đăng xuất, refresh giữ phiên (khi Ghi nhớ).
- [ ] Header, logo, đăng nhập, đăng ký, hồ sơ, menu giữ phong cách giao diện gốc.
- [ ] Mobile 375px, 390px, 768px: không tràn lịch, không che nút, menu tài khoản cuộn được.
- [ ] Chọn/bỏ ngày và Chọn tất cả, sau đó Lưu đăng ký; F5 vẫn giữ dữ liệu.
- [ ] Ngày quá khứ, hôm nay, cuối tuần và ngày mai sau 14:45 khóa đúng; ngày xa hơn vẫn chỉnh được.
- [ ] Thứ Sáu có popup chọn suất thường/đặc biệt 40.000đ; tổng tiền đổi đúng.
- [ ] Popup thực đơn mở/đóng được; upload ảnh Admin rồi User xem thấy sau reload.
- [ ] Hồ sơ đổi tên/số điện thoại/mật khẩu, refresh/đăng nhập lại đúng.
- [ ] Mua thêm vé → trạng thái Chờ duyệt → Admin duyệt → tổng vé/tiền cập nhật.
- [ ] Chợ vé → đăng bán → tài khoản khác mua → tiền của người bán/người mua cập nhật; không hủy giao dịch đã hoàn tất.
- [ ] Tra cứu vé theo phòng/ngày hiển thị Có/Không và tổng số.
- [ ] Admin Tổng quan thống kê hôm nay/ngày mai và bảng các ngày trong tháng.
- [ ] Danh sách nhân viên tìm/lọc, thêm/sửa/khóa, xem lịch tháng/tiền chi tiết.
- [ ] Admin Đăng ký ăn mặc định chỉ người đã đăng ký; có tùy chọn xem tất cả.
- [ ] Admin Phiếu theo phòng có số lượng tổng, nhấn phòng xem nhân viên.
- [ ] Admin Báo cáo mở rộng ngày/tháng/khoảng ngày, xuất XLSX tổng quan và danh sách người đăng ký.
- [ ] Import Excel có mẫu, xem trước, báo lỗi trùng; không nhập khi có lỗi.
- [ ] Nhật ký giao dịch vé hiển thị đầy đủ người bán/mua/ngày/trạng thái.
- [ ] Mở Console/Network không có lỗi JS, yêu cầu API trả dữ liệu đúng và lỗi kết nối hiển thị rõ.
- [ ] `npm.cmd run build` thành công trước khi bàn deploy Vercel.

Xem [kết quả kiểm thử](../../docs/verification.md).
