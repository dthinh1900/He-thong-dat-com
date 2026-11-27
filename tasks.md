# Công việc và test gate

Trạng thái: TODO → IN_PROGRESS → RED → GREEN → VERIFIED, hoặc BLOCKED. Gate thực nghiệm ghi tại [docs/verification.md](docs/verification.md). Chi tiết UC xem [spec](spec.md).

| ID | Phụ thuộc | Phạm vi | Given / When / Then | Lệnh/kết quả kỳ vọng | Trạng thái |
|---|---|---|---|---|---|
| P0 | — | Kiểm kê route, repo, docs và cấu hình | Cây có thay đổi → xem diff và references → giữ các luồng có requirement | git status/diff, rg, Maven/npm baseline | VERIFIED |
| P1 | P0 | Backup, DB local, Flyway, seed | DB loopback đúng schema → seed lặp lại → giữ account/giá cũ | PostgreSQL read-only, pg_dump/pg_restore, seed runner | VERIFIED |
| G1 | P1 | U01/U02/A01/A06 | Active user → login/profile; user gọi admin → 403; admin CRUD/import → đọc lại người dùng thật | Maven, HTTP 401/403/409, API, browser | VERIFIED |
| G2 | P1 | U03/U04/U06/A04 | Biên 14:45 và giao năm → quyền, giá và trạng thái đúng; Admin từ ngày mai | Maven fixed-clock, API, chọn tất cả trên browser | VERIFIED |
| G3 | P1 | U05/A05 | Admin upload ảnh tuần → User đọc đúng ảnh; ảnh sai bị từ chối | API multipart, byte comparison, browser popup | VERIFIED |
| G4 | G2 | U07–U09/A07 | Pending → duyệt/từ chối; bài nhiều vé → mua từng phần/hai buyer đồng thời/hủy còn dư; roster đúng tổng | JUnit, HTTP concurrent, SQL, browser cả hai mode | VERIFIED |
| G5 | G4 | A02/A03/A08 | Vé nhiều loại/nguồn → drill-down, export → khớp DB theo chủ hiện tại | Maven, browser detail, kiểm tra XLSX và SQL | VERIFIED |
| P4 | G1–G5 | Regression, cleanup và handoff | Mock/API → cùng UI; desktop/mobile; build → API origin trực tiếp | Maven package, Vite build/preview, browser 320/375/430 và desktop, git diff --check | VERIFIED local |

Docker build, onboarding trên máy sạch và smoke trên hosting nằm ngoài gate đã chạy local. Bằng chứng và giới hạn xem [biên bản](docs/verification.md).

| ID | Given / When / Then | Trạng thái |
|---|---|---|
| UI-01 | Có lịch và dữ liệu tra cứu → mở ở 1280/1366/1440 px → khung giữa với hai tab như fork, mỗi lần hiển thị một khu vực. Ở 320/375/430 px → bấm nút điều hướng → chọn mục trong danh sách dọc. | VERIFIED API và Mock |
| UI-02 | Có tài khoản Admin → mở quản trị ở 375 px → chọn trang và mục giao dịch bằng menu dọc. Có tài khoản nhân viên → mở lịch hoặc đường dẫn `/thuc-don` → cùng xem và đóng popup trong lịch. | VERIFIED API và Mock |
| SQL-01 | Tháng có giá đổi giữa kỳ → đọc lịch → một truy vấn giá, biên hiệu lực đúng và không đổi snapshot. So sánh SELECT trực tiếp và stored function bằng EXPLAIN local. | VERIFIED |
| QA-02 | Dữ liệu fixture có backup → dùng trình duyệt và bộ API smoke → đọc lại trạng thái vé, giá và người sở hữu qua PostgreSQL. | VERIFIED local |
| DOC-01 | Người dùng mới → đọc README → tìm được hướng dẫn sử dụng, cài local và triển khai. | VERIFIED |
| GIT-01 | Có repo `Mur1701/he-thong-dat-com` và người dùng yêu cầu `ver2` → kiểm tra nội dung phát hành → push nhánh `ver2`. | Đích đã xác minh |
| PRICE-01 | DB có giá 30.000đ/50.000đ → chạy V5–V6 → giá hiện hành và tương lai 25.000đ/40.000đ cùng dữ liệu lịch sử. Admin lưu giá từ hôm nay hoặc một mốc xen giữa → đọc đúng các kỳ giá và giữ số tiền đã phát hành. | VERIFIED local |
| FRIDAY-01 | Thứ Sáu còn hạn → chọn hoặc đổi suất trước 14:45 thứ Năm → lưu và mở lại đúng loại/giá. Từ 14:45 hoặc chọn ngày khác thứ Sáu → API từ chối. | VERIFIED local |
| MENU-01 | Admin có JWT → tải ảnh → backend khởi động lại → tài khoản nhân viên đọc đúng ảnh tuần từ PostgreSQL. | VERIFIED local |
| PRICE-02 | Có giá hiệu lực và đơn cũ → mở Mua thêm → báo giá trùng lịch, lịch sử giữ số tiền lúc gửi. Giá đổi sau khi xem → gửi giá cũ → 409 và có thể tải lại. | VERIFIED local |
| UI-03 | Các phòng có độ dài mã và tên khác nhau → mở bảng nhân viên và đăng ký → cột thẳng hàng, nội dung dài xuống dòng, bảng cuộn trong khung trên điện thoại. | VERIFIED API và Mock |
| PRICE-03 | Vé đăng ký khả dụng còn giá cũ, giá hiệu lực là 25.000đ/40.000đ → áp dụng V7, mở lịch → một suất thường 25.000đ, hai thường 50.000đ, thường và đặc biệt 65.000đ. Lưu và tải lại giữ tổng, vé mua thêm và lịch sử giữ số tiền đã ghi. | VERIFIED local |
| UI-04 | Có thứ Sáu còn được chọn → mở lịch desktop → ghi chú ghim bên phải. Trên điện thoại, ghi chú vừa màn hình và nằm trước nút lưu. | VERIFIED API và Mock |
| RELEASE-01 | Backend/DB thật có sẵn → mở FE và đăng nhập → dùng API JWT. Backend mất kết nối → hiển thị lỗi. DB trống và có cấu hình cấp Admin → chỉ tạo quản trị viên đã khai báo. | VERIFIED local |
| V3-01 | Có vé của nhiều người và nhiều trạng thái → người dùng chọn tháng trong Hồ sơ → chỉ xem vé do mình sở hữu, tổng lấy giá lưu trên vé hợp lệ. | VERIFIED local |
| V3-02 | Có vé hợp lệ, yêu cầu chờ và báo bếp đã ghi → Admin mở Tổng quan → thấy việc cần làm và chênh lệch từng loại suất. Người dùng gọi API báo bếp → 403. | VERIFIED local |
| V3-03 | Người dùng và Admin mở menu tài khoản → các mục công việc thường dùng nằm trước Hồ sơ và Đăng xuất. | VERIFIED local |
| STRUCT-02 | Cùng dữ liệu PostgreSQL → mở các mục quản trị trên bản trước và sau khi chia nhỏ → DOM các màn đã đối chiếu và CSS khớp. Controller gọi interface → đọc đúng dữ liệu, ghi báo bếp, đăng ký và mua thêm thành công. | VERIFIED local |
| STRUCT-03 | Cùng dữ liệu → mở đăng ký, hồ sơ, mua thêm, chợ vé và báo cáo trước/sau khi tách → giữ route, bố cục và kết quả API. Domain và use case → thử với adapter thật và giả → hướng phụ thuộc đi vào lớp application. | VERIFIED local |
| STRUCT-04 | Đăng ký, mua thêm, chuyển nhượng, nhân sự, thực đơn, giá và báo cáo gọi qua interface → chạy cùng dữ liệu → giữ API, giao diện, transaction và Flyway. Core biên dịch độc lập với Spring và JPA. | VERIFIED local |
| UI-05 | Người dùng đã đăng nhập → mở Thông tin cá nhân hoặc Đối soát trên desktop và điện thoại → chỉ thấy mục đã chọn, mở Đối soát qua liên kết cũ, giữ bản nháp và bộ lọc tháng khi chuyển mục. | VERIFIED local |
| UI-06 | Có vé tự hủy, Admin hủy và vé còn hiệu lực cùng ngày → mở lịch, dấu × hoặc nút Vé đã hủy → thấy nguồn, thời điểm, lý do nếu có và giá cũ, tổng chỉ tính vé hợp lệ. Đăng ký lại được khi còn hạn. Đối chiếu kiểu chữ trên các màn chính desktop và mobile. | VERIFIED local |
| UI-07 | Admin đổi và lưu hai giờ chốt → mở lại lịch → hiện hai mốc từ PostgreSQL và ngày mai dùng đúng hạn theo thao tác. Mở hai biểu đồ → chỉ thấy cột thứ Hai đến thứ Sáu, tổng và bộ lọc còn đúng. Menu tài khoản → một lối vào Thông tin cá nhân, mở Đối soát bằng tab hoặc liên kết cũ. | VERIFIED local |
| UI-08 | Chọn hoặc bỏ chọn → tải lại trước khi lưu → DB và màu đã đăng ký giữ như cũ. Lưu bỏ chọn trước hạn → ngày trở về chưa đăng ký, không có dấu ×. Tại hạn → lưu bỏ chọn bị chặn. Khi Admin hủy vé đã khóa → xuất hiện dấu ×, lý do và thời điểm. | VERIFIED local |
