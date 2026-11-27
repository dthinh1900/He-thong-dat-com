# Đặc tả sprint — Hệ thống đăng ký cơm trưa Vietnam Post

Nguồn theo thứ tự ưu tiên: yêu cầu sprint của người dùng, SRS 2.0 đính kèm, [quyết định](docs/decisions.md). Bảng dưới ghi trạng thái baseline. Kết quả thực thi được ghi trong [kiểm thử](docs/verification.md).

USER quản lý hồ sơ và vé của mình, xem thực đơn, yêu cầu mua thêm, chuyển nhượng và tra cứu phòng ban chỉ đọc. ADMIN quản trị trong năm nhóm hiện có: Dashboard, Nhân viên/Phòng ban, Thực đơn/Giá, Đăng ký/Giao dịch, Báo cáo. Giữ logo Vietnam Post, lịch vàng/cam và SPA. Ứng dụng dùng Spring Boot, JWT và PostgreSQL. Cài mới có thể chủ động khởi tạo tài khoản quản trị đầu tiên bằng cấu hình của người vận hành. Ngoài phạm vi: cổng thanh toán, tối ưu hạ tầng quy mô lớn và triển khai ra ngoài workspace.

## Quy tắc

- Giao diện dùng bố cục của fork `Mur1701/He-thong-dat-com-v1-fork` (`0732da3`). Desktop dùng khung 812 px và hai tab Đăng ký/Tra cứu, tổng tiền bên phải lịch, nút lưu bên dưới. Mobile dùng nút mở danh sách dọc cho điều hướng tài khoản, đăng ký và Admin. Nhân viên xem ảnh thực đơn tuần trong hộp thoại của lịch; đường dẫn cũ mở trực tiếp hộp thoại. Admin vẫn quản lý thực đơn và giá vé. Tab trình duyệt dùng tên “Đặt cơm trưa”.

- Người dùng và quản trị dùng chung phông chữ, phân cấp tiêu đề, biểu mẫu, bảng và chú thích theo cùng bộ kiểu chữ. Giữ bố cục và màu nhận diện hiện tại.

- Hai biểu đồ suất ăn theo ngày trong Tổng quan và Báo cáo hiển thị cột từ thứ Hai đến thứ Sáu. Bộ lọc chi tiết, tổng tiền và tệp Excel vẫn phản ánh dữ liệu của toàn kỳ.

- Menu tài khoản mở Thông tin cá nhân để xem và cập nhật hồ sơ, kể cả đổi mật khẩu. Given người dùng đăng nhập, When mở hồ sơ hoặc dùng liên kết đối soát đã lưu, Then hiển thị biểu mẫu cá nhân và không gửi yêu cầu đối soát.

- **BR-01/02**: Chọn hoặc bỏ chọn ngày và loại suất là bản nháp. Chỉ Lưu đăng ký mới phát hành vé hoặc ghi bỏ chọn. Trước giờ chốt, bỏ chọn đã lưu đưa ngày về trạng thái chưa đăng ký, loại khỏi tổng và giữ sự kiện trong nhật ký. Hạn bỏ chọn là mốc sớm hơn giữa giờ chốt đăng ký và giờ bỏ chọn cấu hình, mặc định 14:45 ngày liền trước theo giờ Việt Nam. Backend từ chối thay đổi đến sau hạn. Ngày hiện tại, quá khứ và cuối tuần vẫn bị khóa.
- **BR-03**: Giá chuẩn là 25.000đ cho suất thường và 40.000đ cho suất đặc biệt thứ Sáu. Giá áp dụng theo loại và ngày ăn. Điều chỉnh các mức cũ 30.000đ và 50.000đ cho kỳ hiện hành và kỳ tương lai, giữ lịch sử giá và số tiền các vé đã phát hành. Admin có thể sửa hoặc thêm giá từ hôm nay, kể cả khi đã lên lịch cho ngày sau. Người dùng có thể chọn hoặc đổi suất cho bất kỳ thứ Sáu tương lai nào trước giờ chốt đăng ký của thứ Năm liền trước, mặc định 14:45 giờ Việt Nam. Tổng chi phí tính theo vé hợp lệ của chủ sở hữu hiện tại.
- **BR-03a**: Điều chỉnh một lần các vé đăng ký tháng còn khả dụng của chính người đăng ký cho ngày hiện tại và tương lai, khi vé còn mang mức cũ và bảng giá của đúng ngày đã áp dụng mức chuẩn mới. Vé mua thêm, vé đã chuyển nhượng, vé đang bán, vé đã sử dụng/hủy và ngày đã qua giữ số tiền đã ghi. Tổng lịch bao gồm số tiền thực tế của các vé hợp lệ. Trên màn hình rộng, mục chọn suất thứ Sáu nằm trong ghi chú ghim bên phải khung lịch. Màn hình hẹp đặt ghi chú trước nút lưu.
- **BR-05**: Ảnh thực đơn đã tải lên được lưu cùng dữ liệu tuần trong PostgreSQL. Ứng dụng triển khai sử dụng DB có lưu trữ bền vững để mọi thiết bị đăng nhập có thể xem ảnh qua API sau khi backend khởi động lại.
- **BR-04**: Sau khi vé đã khóa, nhân viên liên hệ quản trị viên. Chỉ Admin thực hiện hủy vé còn hiệu lực từ ngày mai và có thể ghi lý do tối đa 500 ký tự. Với vé tháng cùng chủ còn sửa được, Admin có thể bỏ chọn hộ. Chỉ vé Admin hủy được đánh dấu vàng nhạt và dấu × trên lịch. Vé hủy được giữ trong lịch sử, có thời điểm, lý do nếu có, giá đã ghi và tiền tính bằng 0. Nếu cùng ngày có vé hợp lệ, màu và tổng vẫn phản ánh vé đó.
- Chợ vé chỉ có bài BÁN (yêu cầu sprint thay SRS về bài CẦN MUA). Chọn ngày/loại/số vé thật, giữ chỗ, người khác xác nhận nhận một phần hoặc toàn bộ. Chủ bài có thể bỏ phần chưa khớp. Trong một transaction, kiểm tra quyền sở hữu/tồn và khóa dữ liệu, chuyển chủ, cập nhật số dư bài, tạo giao dịch và audit. Không hoàn tác lượt nhận đã chốt.
- Mua thêm dùng cùng bảng giá theo ngày ăn và loại suất như lịch đăng ký. Hiển thị đơn giá hiện hành trước khi gửi và số tiền lúc gửi trong lịch sử. Nếu giá thay đổi giữa lúc xem và gửi, API báo 409 để người dùng xem lại. Mua thêm có số lượng/lý do, trạng thái chờ, duyệt hoặc từ chối bởi ADMIN. Khi duyệt, phát hành vé và ghi audit cùng transaction từ số tiền đã ghi trên yêu cầu. Trạng thái lỗi API gồm 401 (phiên), 403 (quyền), 400 (dữ liệu/giờ), 404 (không tồn tại), 409 (xung đột).
- Các bảng Admin dùng cùng vị trí cột giữa các phòng ban khi cùng loại dữ liệu. Nội dung dài xuống dòng trong ô. Trên màn hình hẹp, bảng rộng cuộn trong khung bảng.

## Yêu cầu tái cấu trúc

Luồng nghiệp vụ đi qua interface đầu vào và đầu ra. Domain và use case giữ quy tắc độc lập với Spring và PostgreSQL. Adapter giữ HTTP, dữ liệu, ảnh, Excel và transaction. Các route, dữ liệu phản hồi, giao diện và schema hiện tại là hợp đồng cần giữ qua lần tái cấu trúc này.

## Ma trận nghiệm thu

| UC | Baseline | Kỳ vọng và Given → When → Then | API / PostgreSQL | FE / Gate |
|---|---|---|---|---|
| U01 | Implemented | Tài khoản active → đăng nhập → JWT đúng role, thất bại 401, logout hết phiên UI | auth/login; users | Login/Header, G1 |
| U02 | Implemented | Có JWT → cập nhật tên/liên hệ/mật khẩu → tải lại còn và pass mới đăng nhập được | users/me; users | Profile, G1 |
| U02a | API giữ tương thích | Đối soát tiền ăn đã rút khỏi trang hồ sơ theo yêu cầu. API và dữ liệu vé vẫn dùng được theo hợp đồng hiện tại. | users/me/meal-history; meal_tickets | API |
| U03 | Partial | Có vé thường, phát sinh, chuyển nhượng → mở tháng → lịch và chi phí đúng chủ | meal-registrations/month; meal_tickets | Registration, G2 |
| U04 | Partial | Trước/sau 14:45, cuối tuần, giao năm → đăng ký/hủy/chọn tất cả → chỉ ngày hợp lệ đổi; giữ lịch sử | meal-registrations, meal-tickets; meal_registrations, meal_tickets, audit_logs | Calendar, G2 |
| U05 | Implemented | Admin đã upload → mở popup → xem đúng ảnh tuần, xử lý rỗng | menus; menu_images | Calendar/Menu, G3 |
| U06 | Partial | Thứ Sáu chưa đến hạn → chọn hoặc đổi suất → lưu cùng vé với giá hiệu lực. Đúng 14:45 thứ Năm → từ chối đổi; vé đã giữ nguyên loại giữ giá đã ghi | meal-registrations/month; meal_prices, meal_tickets | RegistrationSummary, G2 |
| U07 | Missing | Gửi đúng số lượng/lý do → pending, Admin duyệt → phát hành đúng số và tiền; từ chối không tạo vé | ticket-purchases, admin/purchases; ticket_purchases, meal_tickets, audit_logs | Purchase/Admin, G4 |
| U08 | Missing | Có vé của mình → đăng bán, nhận một phần/cạnh tranh → một chủ mỗi vé, cập nhật tiền và tồn đúng, hủy phần còn dư | ticket-exchanges; ticket_exchanges, ticket_exchange_transactions, meal_tickets, audit_logs | Exchange, G4 |
| U09 | Missing | Chọn ngày/phòng → hiện danh sách và số người/vé chính xác, chỉ đọc | departments, departments/roster; departments, users, meal_tickets | Department panel, G4 |
| A01 | Implemented | USER gọi admin → 403; ADMIN vào đúng bảng điều khiển | auth/login, admin/*; users | Admin route, G1 |
| A02 | Partial | Có các vé đủ loại → Dashboard hôm nay/mai và drill-down → tổng/chi tiết nhất quán | admin/overview, admin/reports/detail; users, meal_tickets | Admin Dashboard, G5 |
| A02a | Bố cục main | Given Admin đã đăng nhập → When mở Tổng quan → Then thấy thẻ đăng ký hôm nay, ngày mai ngay dưới tiêu đề, tiếp theo là thống kê nhân viên, phòng ban và biểu đồ. Các API và dữ liệu ghi nhận số báo bếp được giữ. | admin/overview, admin/kitchen-counts | Admin Dashboard |
| A03 | Partial | Chọn nhân viên/phòng/khoảng ngày → danh sách lịch và tổng đúng snapshot | admin/employees/{id}/history, admin/reports/detail; users, departments, meal_tickets | Admin, G5 |
| A04 | Partial | Có vé ngày mai và hôm nay → Admin hủy → chỉ vé ngày mai đủ điều kiện đổi trạng thái | admin/meal-tickets/{id}/cancel, admin/registrations; meal_tickets, audit_logs | Admin history/registration, G2 |
| A05 | Implemented | Upload PNG/JPEG hợp lệ → User xem ảnh; ảnh sai/quá lớn bị chặn | admin/menus, menus; menu_images | Admin menu/User popup, G3 |
| A06 | Partial | Tạo/sửa/khóa nhân viên và phòng, import XLSX → DB lưu thật, giữ lịch sử | admin/employees, admin/departments, admin/employees/import; users, departments | Admin People, G1 |
| A07 | Missing | Đăng/nhận/hủy bài → ADMIN xem giao dịch và audit, USER không truy cập audit | admin/exchange-transactions, admin/exchange-audit; ticket_exchange_transactions, audit_logs | Admin transactions, G4 |
| A08 | Partial | Lọc khoảng ngày/tháng, phòng, nhân viên, loại/nguồn → drill-down tới vé, xuất .xlsx cho đối soát, tránh trùng khi đổi chủ | admin/reports/detail, admin/reports/range/export, admin/reports/month/export; meal_tickets, users, departments, transactions | Report Dashboard, G5 |
