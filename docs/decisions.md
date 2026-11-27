# Quyết định sprint

| ID | Vấn đề / bằng chứng | Quyết định / phương án bỏ qua | Ảnh hưởng và rủi ro | Cách điều chỉnh | Trạng thái |
|---|---|---|---|---|---|
| D-01 | SRS có bài cần mua, yêu cầu sprint chỉ đăng bán | Dùng SELL; giữ lịch sử BUY trong DB | Chợ, API, audit. Bài BUY cũ không xuất hiện trên chợ | Quyết định sản phẩm mới nếu cần mở rộng | AUTO |
| D-02 | Quy tắc thứ Sáu và giá theo yêu cầu sprint | 25.000 mặc định cho NORMAL khi chưa có giá. SPECIAL lấy DB, chỉ chủ động chọn trong khung thứ Năm | Seed, lịch, báo cáo. Giá cấu hình trước giữ nguyên | Admin đặt giá hiệu lực mới | Thay bằng D-10 |
| D-03 | SRS có ví dụ giờ mua thêm và hướng duyệt | Cho gửi/duyệt vé của ngày hiện tại hoặc tương lai, giới hạn ngày làm việc, Admin duyệt thủ công. Giờ 09:00 trong SRS là ví dụ | Có thể ghi nhận yêu cầu trong ngày | Bổ sung deadline khi được chốt | AUTO |
| D-04 | Schema đã có tổng giá của yêu cầu và giá riêng từng vé. Chợ cần nhóm theo bài | Giữ tổng từ lúc gửi và ghi liên kết bảng giá. Mỗi hàng bán là một vé, khóa theo nhóm và lưu tiền từng giao dịch | V4 cộng thêm. Giữ giá tổng cũ nếu có. Quy trình chốt giá cần review trước khi dùng thực tế | Khôi phục app trước, giữ cột và dữ liệu | REVIEW |
| D-05 | SRS giới hạn Admin từ ngày mai | Admin hủy vé khả dụng từ ngày mai, kể cả sau giờ chốt người dùng. Bài bán đang mở được hủy phần chưa khớp qua luồng chủ bài trước | Lịch Admin và quyền; giữ giao dịch đã khớp | Điều chỉnh chính sách tập trung | AUTO |
| D-06 | Prompt yêu cầu proxy mặc định 8080, máy đã có dịch vụ trên port đó | Mặc định repo 8080, override local 8081 nếu port bận | Chỉ cấu hình chạy local | Đổi PORT/VITE_API_PROXY_TARGET đồng bộ | AUTO |
| D-07 | Ảnh 2 giữ khung lịch gốc. Kiểm thử 1280 px cho thấy rule cũ ẩn tra cứu | Giữ khung lịch 812 px có tra cứu bên trái, tổng tiền bên phải và tab dưới 1180 px | CSS, bảng Admin và tiêu đề trình duyệt | CSS và entry HTML | Thay bằng D-09 |
| D-08 | Tải lịch gọi giá 2 lần/ngày. Thử stored function tạm cho cùng kế hoạch đọc bảng | Gom lịch giá tháng bằng JPQL tham số hóa và rollback mẫu stored function | 56–62 lượt truy vấn giá còn 1. Phân quyền và giá snapshot giữ nguyên | So sánh plan trên dữ liệu đại diện trước khi thêm stored function lâu dài | VERIFIED local |
| D-09 | Người dùng chọn lại giao diện của `Mur1701/He-thong-dat-com-v1-fork` tại `0732da3` | Khung giữa và hai tab trên desktop, điều hướng dạng danh sách dọc trên điện thoại. Giữ popup trong lịch làm điểm xem thực đơn cho nhân viên | Route `/thuc-don` chuyển đến popup, dùng chung API hiện tại | `CompactNavigation`, CSS và route đăng ký | VERIFIED local |
| D-10 | Giá đúng 25.000đ/40.000đ và đổi suất thứ Sáu từ các ngày trước | V5–V6 sửa mức cũ theo kỳ; panel chỉ hiện ngày còn hạn đăng ký, mặc định chốt 14:45 thứ Năm | Số tiền đã ghi giữ nguyên. Đổi loại suất tính giá theo loại và ngày ăn | Admin đặt mốc giá, giờ chốt hoặc tải ảnh theo quy trình hiện tại | VERIFIED local |
| D-11 | Lịch hiển thị 25.000đ/40.000đ nhưng tính theo mức 30.000đ/50.000đ còn lưu trên vé đăng ký | V7 sửa một lần vé còn khả dụng của chính người đăng ký cho ngày hiện tại và tương lai, khi giá hiệu lực đã là mức chuẩn mới | Tổng phản ánh giá ghi trên vé sau khi sửa. Vé mua thêm, đã chuyển nhượng, đang bán, đã hủy và các ngày đã qua giữ lịch sử | Backup trước V7 và SQL đối soát | VERIFIED local |

## Kiểm kê cleanup

| Candidate | File/symbol | Dùng hiện tại | SRS / cần sau tích hợp | Bằng chứng | Quyết định |
|---|---|---|---|---|---|
| UI chức năng mở rộng | frontend/src/preview | Chuyển sang frontend/src/features theo nghiệp vụ | Có | AppRoutes, RegistrationPage, AdminPage | KEEP |
| Dữ liệu demo | Transport Mock, seed tài khoản mẫu, script fixture | Đã gỡ ở ver2 | Frontend gọi API, cấp Admin từ cấu hình khi cài mới | Browser smoke API và kiểm tra startup | DELETE |
| Adapter mock-only cũ | frontend/src/preview/previewService.js và listener riêng | Không còn | Chức năng đã đi qua shared services | `rg` xác nhận hết import; test mock và browser API/Mock xanh | DELETE |
| UI Admin chia đôi theo mode | frontend/src/preview/AdminExtras.jsx | Không còn | Giữ các luồng nghiệp vụ qua AdminPage, AdminBulkImport, AdminTicketTransactions, ReportDashboard | Tìm references và chạy hai mode | DELETE |
| Style placeholder API | registration-lookup-card__api-note | Không còn | Tra cứu dùng API/Mock cùng component | Tìm references và browser test | DELETE |
| Tệp rỗng | `git` tại root, 0 byte | Không | Không | Lịch sử Git và tìm references; Maven/Vite regression | DELETE |
| Schema lịch sử | Flyway V1–V3, bảng BUY | Database đang dùng | Cần bảo toàn | Migrations, schema và audit | KEEP |

Thêm bằng chứng từng đợt vào bảng này khi kiểm thử.
