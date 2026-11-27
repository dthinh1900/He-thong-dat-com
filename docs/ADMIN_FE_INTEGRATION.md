# Tích hợp Frontend Admin với Backend API

## Các màn hình đã nối API

| Màn hình FE | Endpoint chính | Cách sử dụng |
|---|---|---|
| Tổng quan | `GET /api/admin/overview` | Hiển thị số nhân viên có vé hợp lệ hôm nay/ngày mai, tổng nhân viên và phòng ban |
| Phòng ban | `GET/POST/PUT /api/admin/departments` | Tải danh sách, tạo và sửa phòng ban |
| Nhân viên | `GET /api/admin/employees`, `GET /api/admin/employees/{id}`, `POST/PUT /api/admin/employees` | Tải danh sách/chi tiết, tạo và cập nhật nhân viên |
| Import Excel | `POST /api/admin/employees/import` | Đọc XLSX và tạo danh sách nhân viên trong một transaction |
| Khóa/mở khóa | `PATCH /api/admin/employees/{id}/active` | Cập nhật trạng thái tài khoản |
| Ngừng tài khoản | `DELETE /api/admin/employees/{id}` | Backend thực hiện soft-retire (`active=false`), không xóa cứng dữ liệu lịch sử |
| Đăng ký ăn | `GET/PUT /api/admin/registrations` | Tải danh sách theo ngày/phòng và bật/tắt một vé đăng ký theo tháng |
| Thời hạn | `GET/PUT /api/admin/registration-settings` | Đọc và lưu giờ chốt đăng ký và hạn bỏ chọn, với mốc bỏ chọn hiệu lực không vượt quá giờ chốt đăng ký |
| Thực đơn | `GET/PUT /api/admin/menus`, `GET /api/menus/{weekStart}/image` | Đọc metadata/ảnh có Bearer token, tải ảnh PNG/JPEG tối đa 5 MB lên PostgreSQL |
| Lịch sử cá nhân | `GET /api/admin/employees/{id}/history`, `PATCH /api/admin/meal-tickets/{id}/cancel` | Mở tháng, đối soát, bỏ chọn trước hạn hoặc hủy vé đã khóa từ ngày mai |
| Mua thêm và chợ vé | `GET/PATCH /api/admin/purchases`, `GET /api/admin/exchange-transactions`, `GET /api/admin/exchange-audit` | Duyệt, từ chối và xem nhật ký |
| Giá | `GET/POST /api/admin/meal-prices` | Xem hoặc bổ sung giá hiệu lực theo ngày |
| Báo cáo | `GET /api/admin/reports/month`, `GET /api/admin/reports/detail` | Xem từ ngày đến vé cụ thể, dùng giá lưu trên vé |
| Xuất Excel | `GET /api/admin/reports/range/export`, `GET /api/admin/reports/month/export` | Tải tổng hợp hoặc xuất bảng chi tiết đang lọc từ dữ liệu API |

Tất cả endpoint `/api/admin/**` cần JWT của tài khoản có role `ADMIN`. Xem [quickstart](../README.md) để cấu hình Vite proxy và URL.

## Quy tắc nghiệp vụ cần lưu ý

- Tạo nhân viên bắt buộc có mật khẩu ban đầu; FE yêu cầu tối thiểu 8 ký tự theo DTO Backend. Khi sửa, để trống mật khẩu nếu không muốn đổi.
- Backend không xóa cứng nhân viên. Endpoint DELETE chỉ ngừng hoạt động tài khoản để giữ vé và lịch sử.
- Backend không cho Admin tự khóa/hạ quyền chính mình hoặc khóa/hạ quyền Admin hoạt động cuối cùng.
- API màn hình đăng ký ăn chỉ đọc/thay đổi vé có `registration_id`; vé mua thêm có `purchase_request_id` không nằm trong thao tác đăng ký/hủy này.
- Dashboard đếm người sở hữu vé có trạng thái `AVAILABLE`, `LISTED_FOR_EXCHANGE` hoặc `USED` vào ngày tương ứng.
- Báo cáo tháng tính vé ở các trạng thái hợp lệ, cộng giá snapshot `meal_tickets.price`, và phân nhóm theo phòng ban hiện tại của nhân viên.
- Báo cáo là bản tổng hợp từ vé, không phải sổ quyết toán; schema hiện tại không lưu phòng ban lịch sử tại thời điểm phát sinh vé.
- Backend kiểm tra giờ chốt và ngày phục vụ. Admin hỗ trợ hủy vé khả dụng từ ngày mai kể cả sau giờ chốt của USER.

## Kiểm tra thủ công sau khi chạy

1. Đăng nhập bằng tài khoản `ADMIN`, mở Chức năng admin và kiểm tra Dashboard có số liệu từ PostgreSQL.
2. Tạo/sửa phòng ban; tạo nhân viên với mật khẩu ban đầu; sửa thông tin; khóa/mở khóa và ngừng hoạt động một tài khoản thử nghiệm.
3. Chọn một ngày tương lai hợp lệ để xem đăng ký ăn; thử đăng ký/hủy và xác nhận thông báo từ Backend. Ngày đã khóa theo chính sách sẽ bị vô hiệu hóa.
4. Tải ảnh PNG/JPEG nhỏ hơn 5 MB lên, lưu, chuyển trang rồi quay lại để xác nhận ảnh được đọc lại từ Backend.
5. Chọn tháng có dữ liệu để kiểm tra biểu đồ và tổng tiền; xuất XLSX và mở file kiểm tra sheet từng phòng.
6. Đăng nhập bằng USER và thử gọi `/api/admin/overview`; Backend phải trả 403. Chưa đăng nhập phải trả 401.
