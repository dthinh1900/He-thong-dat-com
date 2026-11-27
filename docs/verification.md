# Bằng chứng kiểm thử sprint

Workspace local: `D:\Github\He-thong-dat-com-v2`, nhánh `minh/backend-admin`, baseline HEAD `0732da3`. Giữ thay đổi local khi bắt đầu. Kết quả chưa chạy ghi NOT TESTED.

| Gate | Lệnh / bằng chứng | Kết quả |
|---|---|---|
| P0 | Git status, branch, log và diff chỉ đọc | PASS |
| Baseline | `mvn test` và `npm run build` | PASS 17/17; Vite cảnh báo kích thước chunk ExcelJS |
| Test RED | `mvn test -Dtest=TicketOperationsServiceTest` trước khi có service, test mock trước khi có route | RED ghi nhận |
| Backend startup | `mvn spring-boot:run`, Flyway V4 và Hibernate validate. | PASS. Startup RED do tên thuộc tính sort `mealDate.id`, đã sửa thứ tự `mealDate`, `id` và khởi động lại. |
| Backend tests | `mvn package -Ddebug=false` dùng Java 21 và tạo executable JAR. | PASS 28, 0 fail, 0 skip. |
| Local DB | PostgreSQL 18.6, `127.0.0.1:5432/meal_booking_db`, schema `public`, local user `postgres`; Flyway 1–4 xanh. `pg_dump` dạng custom kiểm qua `pg_restore --list`. | PASS. Seed có 4 tài khoản fixture, 5 vé, 2 yêu cầu mua, 2 bài bán, 1 giao dịch và PNG 68 byte trước smoke. Chạy lại báo không ghi thêm. Bảng giá có sẵn 30.000/50.000đ được giữ. |
| G1 — U01, U02, A01, A06 | Login/logout, hồ sơ/đổi mật khẩu, phòng ban, import, tài khoản ngừng hoạt động. USER có JWT gọi Admin từng nhận 401 mặc dù `/api/users/me` trả 200. | PASS. Sau khi sửa HTTP security handler, USER nhận 403, chưa xác thực 401, trùng mã import 409. |
| G2 — U03, U04, U06, A04 | Unit clock tại 14:44/14:45 và chuyển năm, test chọn đặc biệt thứ Sáu, ownership ledger. API đăng ký hợp lệ, từ chối ngày sai, hủy vé Admin và đọc lại trạng thái. | PASS. Trình duyệt thử chọn tất cả khi còn ngày chỉnh sửa. |
| G3 — U05, A05 | API multipart: từ chối ảnh sai, upload PNG ở tuần fixture riêng, đọc lại đúng byte. Mở popup dùng ảnh tuần seed. | PASS. |
| G4 — U07–U09, A07 | `database/smoke-local-api.ps1`, browser purchase/approve/reload, SQL đối soát. | PASS 17/17 assertion toàn bộ bộ smoke API trên JAR. Mua đồng thời trả đúng 200 và 409. Không phát hành lại khi duyệt lần hai. Hủy bài giải phóng vé và ghi audit. |
| G5 — A02, A03, A08 | Bảng tổng quan, JSON lịch và doanh số, drill-down qua cột ngày đến vé nhân viên, XLSX export. | PASS. File chứa workbook và nhiều sheet, roster khớp số vé hợp lệ trong báo cáo. |
| Mock | `node --test tests/ticket-flow.test.mjs`, Edge headless `frontend/tests/browser-smoke.mjs`. | PASS 1/1 test mock; browser không phát request API HTTP. |
| Dev API | Edge headless qua Vite 5175 → backend 8082 → DB, mock và API cùng UI. | PASS desktop 1366px, mobile 320/375/430px. Mất request roster hiển thị lỗi và nút thử lại. |
| Artifact API | `npm run build` nhúng origin HTTPS ví dụ; build thử lần hai với origin localhost, Vite preview 5176 → JAR `prod` 8083 → DB local. | PASS. Preflight 200, refresh route SPA 200, mua/duyệt qua trình duyệt và báo cáo chạy. |
| Deployment được quản lý | Docker engine, máy mới, HTTPS/Render, Vercel, PostgreSQL hạ tầng. | NOT TESTED. Máy hiện tại thiếu Docker CLI, không chạy deploy và không sử dụng DB ngoài localhost. |

## Đối soát PostgreSQL

Truy vấn dùng `BEGIN READ ONLY`; ảnh và phản hồi browser nằm trong `.local/browser-smoke/` (Git bỏ qua). Dữ liệu smoke để lại trong DB local theo mã riêng, giữ dữ liệu đã có trước khi chạy.

| Bản ghi | SQL sau thao tác |
|---|---|
| Mua thêm API, purchase ID 9 | APPROVED, yêu cầu 3 vé, phát đúng 3, tổng và snapshot cùng 90.000đ |
| Bài bán nhóm ID 10 | 3 vé phân biệt, 3 lượt hoàn tất, 90.000đ, từng chủ hiện tại trùng buyer |
| Roster 25/11/2026 tại thời điểm đọc | 16 vé hợp lệ, 4 người sở hữu, 480.000đ |
| Lượt thao tác UI, purchase ID 12 | fixture-purchase, APPROVED, 1 vé phát hành, 30.000đ |

Hai cảnh báo còn lại: ExcelJS nằm trong chunk tải động trên 500 kB; phiên bản Flyway hiện có cảnh báo PostgreSQL 18.6 mới hơn phiên bản đã xác nhận tương thích. Cả build và migration đã xanh trong môi trường này.

## Kiểm tra lại ngày 25/11/2026

- Trước sửa, browser smoke ở 1280 px cho thấy trang desktop bật tab và ẩn bảng tra cứu. Sau sửa, API và Mock đều qua ở 1280/1366/1440 px và 320/375/430 px. Ảnh kiểm tra trong `.local/ui-review/`. Tiêu đề “Đặt cơm trưa” xuất hiện trong trình duyệt và `dist/index.html`.
- Maven: 29/29 test. Vite build và test Mock: PASS. Browser smoke ở hai mode mở lịch/tổng tiền, lưu và hủy ngày, tra cứu, hồ sơ, thực đơn, mua thêm, chợ vé, danh sách/chi tiết nhân viên, giá và quy định, duyệt mua thêm, nhật ký, báo cáo chi tiết và nút xuất Excel. Mode API qua Vite 5173 đến backend 8081. Mock không gọi HTTP API.
- API smoke: 17/17, gồm cập nhật hồ sơ/mật khẩu, import/khóa người dùng, kiểm tra ảnh, phiên/quyền, nhận vé đồng thời, kiểm tra số tiền và file XLSX. Thao tác đổi giá/giờ chốt toàn hệ thống và deploy bên ngoài không nằm trong lượt ghi fixture này.
- DB được xác minh `meal_booking_db` tại `127.0.0.1`, schema `public`, Flyway V1–V4. Trước lúc ghi thử đã tạo backup custom và kiểm `pg_restore --list`, lưu trong `.local/`. Đối soát sau ghi dùng giao dịch chỉ đọc:
  - Browser mua thêm ID 21, trạng thái APPROVED, phát đúng 1 vé giá 30.000đ. Vé đăng ký thử cho 26/11/2026 đã chuyển sang CANCELLED kèm thời điểm hủy.
  - API mua thêm ID 17 đã phát 3 vé, tổng 90.000đ. Bài bán ID 18 ghi 3 giao dịch cho 3 vé khác nhau, tổng 90.000đ, chủ hiện tại khớp người nhận. Roster ngày 27/11/2026 tại thời điểm đối soát có 6 vé thuộc 4 người, tổng 200.000đ.
- Query giá tháng trước đây gọi 2 query/ngày. Test backend mới xác nhận 1 query/tháng và biên ngày hiệu lực. `EXPLAIN (ANALYZE, BUFFERS)` trên bộ fixture trả 2 dòng giá bằng cả SELECT và stored function `SECURITY INVOKER` trong `pg_temp`. Cùng kế hoạch sort + scan và cùng 4 buffer hit khi thực thi. Đây là mẫu dữ liệu nhỏ, không dùng một lần đo để kết luận độ trễ production. Function tạm đã rollback, schema giữ V4.

## Giao diện theo fork ngày 25/11/2026

- Edge headless, API và Mock, `node tests/browser-smoke.mjs --layout-only`: PASS. Kiểm tra khung giữa và chuyển tab tại 1280/1366/1440 px, danh sách dọc tại 320/375/430 px, điều hướng Admin và nhóm giao dịch tại 375 px. Cả nút cạnh lịch và `/thuc-don` mở cùng popup thực đơn. Ảnh tại `.local/fork-layout/`.
- Mock chạy toàn bộ `node tests/browser-smoke.mjs`: PASS, không gọi HTTP API. `node --test tests/ticket-flow.test.mjs`: 1/1. API qua Vite 5173 đến backend 8081 đã đọc lịch, danh sách tra cứu, ảnh thực đơn và dữ liệu Admin. Lượt kiểm tra API này chỉ đọc dữ liệu phục vụ giao diện.
- `mvn test`: 29/29. `npm run build`: PASS với cảnh báo kích thước chunk ExcelJS. `git diff --check`: PASS.
- PostgreSQL `127.0.0.1/meal_booking_db`, schema `public`: phiên bản Flyway 1–4 đều thành công. Truy vấn đối soát dùng `BEGIN READ ONLY` và `ROLLBACK`.

## Giá, suất thứ Sáu và ảnh thực đơn ngày 25/11/2026

- Maven package chạy 30/30 test. Vite build PASS với cảnh báo kích thước chunk ExcelJS. Test Mock 3/3. Kịch bản `browser-smoke.mjs --prices-friday` PASS trên Mock và API qua Vite 5175 đến backend 8084, kiểm tra Admin sửa giá từ hôm nay, nhân viên chuyển vé thường sang đặc biệt và tải lại đúng lựa chọn. Kiểm thử giờ cố định xác nhận thứ Tư và thứ Năm 14:44 cho đổi, 14:45 từ chối và chỉ thứ Sáu có loại đặc biệt. Full browser smoke Mock PASS. Bộ hồi quy API 17/17 trên bản sao DB.
- Đã phục hồi bản sao vào `meal_booking_price_friday_20261125` trước các lượt ghi thử. `SPRING_DATASOURCE_HIKARI_READ_ONLY=true` trên máy gây SQLSTATE `25006` khi lưu giá. Backend chạy ở chế độ ghi cho phiên kiểm thử và thao tác lưu đã thành công. Checksum V5 được giữ khớp `973695657`; V6 đã chạy trên bản sao và DB local gốc. Backup trước khi áp dụng V6 gốc ở `.local/meal_booking_db-before-v6-20261125-093704-805.dump`, đọc được catalog qua `pg_restore --list`.
- SQL chỉ đọc xác nhận giá ngày 27/11/2026 ở cả hai DB là NORMAL 25.000đ và SPECIAL 40.000đ. Vé ID 6 giữ 50.000đ trên DB gốc theo số tiền đã ghi. Cùng vé trên bản sao thành 40.000đ sau thao tác đổi loại trên UI.
- Admin tải PNG lên bản sao và nhân viên mở được từ trình duyệt. Sau khi dừng và chạy lại backend, đăng nhập mới và tải lại ảnh tuần qua API. SHA-256 của file tải về khớp ảnh đã tải lên và dữ liệu PostgreSQL (`5b7f8c6df860d58dc22c4b7f9f8aaba06ca2213b420aefd3f0a4aefd295459d2`). Ảnh hiện có trên DB gốc giữ nguyên. Ảnh kiểm thử ở `.local/price-friday/`. Hạ tầng cloud chưa được kiểm thử.

## Đơn giá mua thêm và bảng theo phòng ban ngày 25/11/2026

- API local đang chạy qua Vite 5173 trả 25.000đ cho suất thường ngày 27/11. SQL chỉ đọc trên `meal_booking_db` xác nhận 25.000đ/40.000đ là giá hiệu lực, các dòng mua thêm 30.000đ trong lịch sử được tạo trước khi sửa giá. Không ghi đơn kiểm thử vào DB gốc.
- Browser smoke đầy đủ PASS trong API và Mock ở 1280/1366/1440 px và 320/375/430 px. Browser API gọi báo giá, gửi đơn và duyệt qua backend. PostgreSQL thử nghiệm xác nhận đơn ID 25 có một vé đã duyệt giá 25.000đ. Lượt kiểm tra API đổi giá cho 11/12 từ 25.000đ lên 26.000đ xác nhận lịch và báo giá cùng đổi, gửi báo giá cũ trả 409, và SQL giữ hai đơn ID 26/27 với số tiền tương ứng 50.000đ/52.000đ. Bản sao kiểm thử đã sao lưu trước khi ghi.
- Maven 30/30, test Mock 4/4 và Vite build PASS. Vite vẫn cảnh báo kích thước chunk ExcelJS. Ảnh trình duyệt ở `.local/order-quote/`.
- Browser `--layout-only` PASS ở cả API và Mock. Đo tọa độ cột bảng nhân viên giữa các phòng ban trên desktop và bảng đăng ký giữa các phòng tại 375 px. Bảng nhân viên cuộn ngang trong khung ở 375 px, trang vừa chiều rộng màn hình. `git diff --check` PASS.

## Ghi chú suất thứ Sáu và tổng tiền lịch ngày 25/11/2026

- Sao lưu `meal_booking_db` vào `.local/meal_booking_before_calendar_rates-20261125-101803-117.dump`, xác minh catalog và phục hồi thành `meal_booking_calendar_rates_20261125` trước khi tạo fixture. Flyway V7 thành công ở cả hai DB. Truy vấn chỉ đọc ở DB gốc xác nhận vé đăng ký ID 4 và ID 6 có giá mới 25.000đ và 40.000đ, đơn mua thêm ID 21 giữ 30.000đ.
- `browser-smoke.mjs --calendar-rates` PASS ở cả API và Mock. Trình duyệt mở một vé giá cũ đã được sửa, cộng thêm một suất thường, đổi sang suất đặc biệt và thấy 25.000đ, 50.000đ, 65.000đ. Sau lưu và tải lại, tổng giữ 65.000đ. PostgreSQL thử nghiệm có đúng hai vé 25.000đ và 40.000đ cho fixture.
- Ghi chú nằm bên phải và dùng `position: sticky` tại 1280/1366/1440 px, nằm trong khung trước nút lưu tại 320/375/430 px. Ảnh ở `.local/calendar-rate-note/`. Maven 30/30, Vite build PASS, Mock 4/4 và hồi quy API 17/17 trên bản sao.

## Bản phát hành ver2

- Maven 25/25, Vite build thành công và `git diff --cached --check` đạt. Đã kiểm tra cây mã nguồn, bản build và danh sách file release để loại adapter demo, dữ liệu mẫu, cấu hình riêng và backup khỏi phiên bản phát hành.
- Browser smoke qua Vite 5175 và backend 8084 chạy với PostgreSQL riêng đã sao lưu. Đăng nhập, lịch, mua thêm, duyệt Admin, báo cáo, export XLSX và hiển thị lỗi khi API tra cứu mất kết nối đều đạt. SQL xác nhận đơn ID 25 ở trạng thái APPROVED, phát hành đúng một vé giá 25.000đ. Bộ hồi quy API 17/17. Flyway V1–V7 xác thực thành công.
- Cài trên DB trống riêng, cấu hình cấp quản trị viên và đăng nhập bằng tài khoản vừa tạo. SQL xác nhận đúng một người dùng có vai trò ADMIN, hai mức giá chuẩn 25.000đ và 40.000đ.

## Main và ver3 ngày 26/11/2026

- Merge commit main `656ba8031a703fb4958e7ab74956d06f74b48ae5` có cây mã giống `ver2`. Vercel production build từ main và đăng nhập hai vai trò tải được lịch qua Render. Admin đọc đủ ba nhân viên mẫu. Render vẫn liên kết với `ver2` tại thời điểm kiểm tra. Neon production có V1–V7.
- Maven test ver3 đạt 25/25 với heap giới hạn. Vite build thành công, còn cảnh báo kích thước chunk ExcelJS. Test PostgreSQL dùng database `meal_booking_ver3_20261126` tại cổng 55442 và có backup trước V8. Flyway validate lịch sử V1–V7 rồi áp dụng V8.
- Đăng nhập nhân viên tại 375 px, menu ưu tiên đăng ký và mở được đối soát. Có 4 vé của chính nhân viên, trong đó 3 vé tính tổng 93.000đ và vé hủy loại khỏi tổng. API quản trị trả 403 cho nhân viên.
- Admin mở việc chờ duyệt từ Tổng quan, ghi số đã báo bếp và duyệt một vé. Trình duyệt hiện phát sinh +1 suất thường. SQL xác nhận đơn APPROVED phát hành đúng một vé 25.000đ, chênh lệch suất đặc biệt bằng 0.

## Tái cơ cấu thư mục ngày 26/11/2026

- Frontend chuyển các mô-đun từ `preview` sang `features` theo nghiệp vụ, giữ giao diện và route. Vite build thành công, CSS đầu ra khớp bản ver3. Diff xác nhận mã backend và migration giống ver3 đã kiểm thử.
- Browser smoke trên cùng PostgreSQL riêng kiểm tra đối soát sau duyệt 118.000đ, mua thêm, chợ vé, tra cứu phòng và mở/đóng hộp thực đơn từ đường dẫn cũ. Admin xem phát sinh +1, danh sách nhân viên, mục nhập Excel và tải báo cáo thành công.
- `git grep` xác nhận script sửa bảng cũ chỉ được nhắc trong tài liệu, không có người gọi trong runtime hay CI. Bản cũ còn trong lịch sử Git. Giữ V0 bootstrap local và V1–V8 của Flyway.

## Render main và tách AdminPage ngày 26/11/2026

- Render deploy `dep-dc3pk9e1egvs73ecg1u0` từ commit main `656ba80` và lên `live`. Vercel production cùng commit. Trình duyệt đăng nhập hai vai trò, tải được lịch và danh sách nhân viên. SQL chỉ đọc trên Neon xác nhận V1–V7 và 4 tài khoản hoạt động.
- `AdminPage.jsx` giảm từ 839 xuống 103 dòng. Mã cho các mục và hộp thoại nằm trong `features/admin`, báo cáo dùng thành phần hiện tại. Hai bản build có cùng hash CSS.
- Edge đối chiếu DOM của Tổng quan, danh sách nhân viên, chi tiết, form sửa, hộp thêm phòng ban, thực đơn và giá, đăng ký, quy định ở 1440 px và 375 px. Các DOM này khớp sau khi chuẩn hóa mã định danh do React tạo và URL ảnh dạng blob. API báo cáo hoạt động ở cả hai bản.
- Maven package đạt 25/25 test, Vite build đạt. Các interface `AdminMeals`, `MealRegistrations`, `TicketOperations` và `TicketQueries` được kiểm tra qua các route đọc và ghi trên `meal_booking_ver3_20261126`, cổng 55442, có backup. API tạo bản ghi báo bếp, đăng ký cho nhân viên và tạo yêu cầu mua thêm. SQL đối soát có một vé AVAILABLE và một yêu cầu PENDING; số suất thường tăng đúng một. USER gọi API Admin trả 403.

## Clean Architecture và giao diện nhân viên ngày 26/11/2026

- Checkout `D:/Github/he-thong-dat-com`, nhánh `refactor/structure`, commit mốc `24ef74d`. Production Vercel/Render tiếp tục phục vụ `main` commit `656ba80`; trang đăng nhập và API health trả 200 trong lần kiểm tra.
- Maven package đạt 28/28. Vite build thành công, hash CSS khớp bản mốc. `jdeps` trên `application` và `domain` xác nhận không có phụ thuộc trực tiếp hướng ra Spring, JPA, controller, adapter, entity, repository hoặc config.
- Edge so sánh DOM bản mốc và bản mới ở 1440 px và 375 px. Lịch, tra cứu, hộp thực đơn, mua thêm, chợ vé, hồ sơ/đối soát, tổng quan Admin, báo cáo và chi tiết vé đều khớp sau khi chuẩn hóa ID React và URL blob. Xuất XLSX từ bảng lọc tạo được dữ liệu ở cả hai bản.
- API thử nghiệm trả 200 khi đăng nhập đúng, 401 khi sai mật khẩu, 400 khi tháng đối soát sai và 403 khi USER gọi Admin. SQL chỉ đọc trên `meal_booking_ver3_20261126`, cổng 55442, xác nhận V1–V8 và 5 vé thuộc nhân viên kiểm thử, trong đó 4 vé tính tiền tổng 118.000đ.

## Clean Architecture cho toàn hệ thống ngày 26/11/2026

- Checkout `D:/Github/he-thong-dat-com`, nhánh `refactor/structure`, mốc `24ef74d`. Maven clean package đạt 28/28 test. Vite build thành công, vẫn có cảnh báo kích thước chunk ExcelJS. `jdeps` xác nhận bytecode của `application` và `domain` chỉ tham chiếu Java và các lớp nghiệp vụ. Diff đạt kiểm tra khoảng trắng. CSS build có cùng SHA-256 với mốc và các migration V1–V8 giữ nguyên.
- Chạy `mvn spring-boot:run` với profile local trên cổng 8086 và `npm run dev` trên 5188. Health trả UP qua proxy Vite. Flyway xác thực đủ 8 migration trên PostgreSQL riêng `meal_booking_ver3_20261126`, cổng 55442. Backup `before-clean-architecture-regression.dump` được xác thực bằng `pg_restore --list` trước khi ghi.
- Edge đối chiếu DOM của lịch, tra cứu phòng, hộp thực đơn, mua thêm, chợ vé, hồ sơ/đối soát, Tổng quan Admin, báo cáo và chi tiết vé ở 1440 px và 375 px. Cả hai kích thước khớp bản mốc sau khi chuẩn hóa ID React và URL blob. Trình duyệt tạo được XLSX từ báo cáo. API trả 200/401 cho thông tin đăng nhập đúng/sai, 400 cho tháng sai và 403 khi USER gọi Admin.
- API ghi và đọc lại hồ sơ, quy định, phòng ban, ảnh thực đơn, giá, đăng ký và số đã báo bếp. Thử mức giá 26.700đ trên DB riêng, SQL và API cùng thấy mức giá đó trên vé đăng ký và vé mua thêm. SQL xác nhận một đăng ký suất đặc biệt thứ Sáu và báo bếp ghi 1 suất thường, 0 suất đặc biệt.
- Hai lượt duyệt đồng thời cho đơn 32 trả 200 và 409. SQL xác nhận đơn APPROVED sinh đúng một vé. Bài bán 27 có đúng một giao dịch COMPLETED, người nhận và chủ vé trùng nhau. Import lỗi ở dòng thứ hai trả 409 và SQL xác nhận không có bản ghi của dòng đầu. Ảnh thực đơn PNG tải lại khớp đúng 7.710 byte. Đối soát tháng 11 vẫn có 4 vé tính tiền tổng 118.000đ.

## Điều hướng Thông tin cá nhân và Đối soát ngày 26/11/2026

- Edge kiểm tra ở 1440, 375 và 320 px. Desktop có hai nút ngang. Điện thoại mở/đóng danh sách dọc bằng nút và phím Escape. Mỗi lần chỉ hiện mục đang chọn. Menu tài khoản và URL cũ mở thẳng Đối soát. Bản nháp hồ sơ và tháng đã chọn được giữ khi chuyển mục. Tab Tra cứu của trang Đăng ký vẫn mở đúng.
- API đối soát không được gọi khi mở Thông tin cá nhân lần đầu. Khi mở Đối soát, API trả 200 và trình duyệt hiển thị 118.000đ cho tháng 11/2026. SQL chỉ đọc trên PostgreSQL riêng cổng 55442 có backup xác nhận 4 vé tính tiền và tổng 118.000đ.
- Maven 28/28, Vite build đạt, Flyway xác thực 8 migration và kiểm tra diff đạt.

## Kiểu chữ chung và vé đã hủy ngày 26/11/2026

- Maven đạt 29/29. Vite build thành công. Kiểm tra diff đạt và các migration V1–V8 giữ nguyên. Flyway áp dụng V9 trên PostgreSQL riêng cổng 55442 sau khi xác minh backup và kiểm tra catalog bằng pg_restore.
- Trình duyệt kiểm tra font, cỡ tiêu đề và độ rộng trang ở 1440, 375 và 320 px trên Đăng nhập, Đăng ký, Tra cứu, Mua thêm, Chợ vé, Thông tin và Đối soát. Báo cáo và bảng Nhân viên cũng được kiểm tra trên Admin, gồm điều hướng ở 375 và 320 px.
- Từ danh sách Admin, đổi ngày và hủy vé với lý do, API trả 200. Nhân viên mở nút Vé đã hủy hoặc dấu × của ngày để xem nguồn, thời gian, lý do, giá đã ghi và tiền tính 0đ. Các ngày chỉ có vé hủy dùng vàng nhạt với viền đứt. Ngày có vé mua thêm còn hiệu lực vẫn vàng đậm với dấu ×. Kiểm tra được bản nháp đăng ký lại khi còn hạn.
- API xác nhận quyền hủy quản trị, tài khoản khác không nhận dữ liệu vé đã hủy của nhân viên này. SQL xác nhận hai vé Admin hủy có lý do trong vé và nhật ký, một vé tự hủy có thời điểm, vé mua thêm cùng ngày vẫn tính 25.000đ. Nội dung đã ghi trên vé hủy cũ vẫn tồn tại sau migration.

## Giờ chốt, biểu đồ ngày làm việc và menu tài khoản ngày 26/11/2026

- Admin lưu giờ đăng ký 07:15 và giờ hủy 23:20 trên PostgreSQL thử nghiệm cổng 55442 đã sao lưu. Trình duyệt ở 1440 và 375 px hiển thị đúng hai mốc khi mở lại lịch. API xác nhận ngày mai khóa đăng ký mới và đổi loại suất sau 07:15, đồng thời vé đã đăng ký vẫn hủy được trước 23:20. SQL xác nhận hai mốc đã đổi và được trả về 14:45 sau kiểm thử.
- Hai biểu đồ tháng 11/2026 chỉ hiển thị 21 cột thứ Hai đến thứ Sáu. Bấm cột mở bộ lọc chi tiết. Kỳ một ngày thứ Bảy hiện thông báo trống của biểu đồ và vẫn có bảng chi tiết. Menu tài khoản chỉ còn mục Thông tin cá nhân dẫn vào trang chung, tab Đối soát và liên kết cũ hoạt động ở cả hai kích thước.
- Maven đạt 29/29, Vite build và kiểm tra diff đạt. Flyway xác thực 9 migration trên DB thử nghiệm.

## Lưu bản nháp, bỏ chọn và hủy vé ngày 26/11/2026

- Edge 1440 px xác nhận chọn ngày mai chỉ đổi viền và tiền dự kiến. API và PostgreSQL vẫn chưa có vé, tải lại web bỏ bản nháp. Bấm Lưu mới sinh vé AVAILABLE và tô vàng. Edge 375 px xác nhận bỏ chọn chưa lưu giữ vàng và vé cũ, tải lại giữ nguyên vé. Lưu bỏ chọn xóa màu đã đăng ký, dấu × vắng mặt và tiền tính về 0.
- Với giờ bỏ chọn cấu hình muộn hơn giờ đăng ký, API dùng mốc sớm hơn. UI khóa vé đã lưu sau hạn. Gửi bản nháp bỏ chọn đến API trả 400, API hủy trực tiếp của nhân viên trả 403, yêu cầu bỏ chọn từ màn Admin cũ trả 409 nếu đã qua hạn. Vé giữ AVAILABLE cho đến khi Admin xác nhận hủy.
- Admin bỏ chọn hộ trước hạn bằng API và hủy đích danh sau hạn qua giao diện. Nhân viên xem dấu ×, lý do, người hủy và 0đ. SQL xác nhận CANCELLED do ADMIN có lý do và thời điểm, hai sự kiện WITHDRAW_TICKET và một CANCEL_TICKET cho cùng mã vé. Kiểm thử khôi phục giờ cấu hình về 14:45. V10 chuyển vé tự bỏ chọn có bằng chứng trong V9 sang WITHDRAWN và giữ metadata cũ.
- Maven 30/30, Vite build và kiểm tra diff đạt. PostgreSQL riêng cổng 55442 có backup được kiểm tra bằng pg_restore trước khi Flyway áp dụng V10.

## Bố cục Tổng quan và hồ sơ ver3 ngày 26/11/2026

- Edge 1440 px: tiêu đề Tổng quan đứng ngay trước hai thẻ hôm nay/ngày mai như main. Có đủ năm mục quản trị. API tổng quan trả 200, trình duyệt không gọi API báo bếp.
- Edge 375 px: nhân viên đăng nhập, mở lịch và liên kết hồ sơ có hash cũ. Form thông tin và đổi mật khẩu hiển thị, không có tab hoặc request đối soát, trang không tràn ngang.
- PostgreSQL thử nghiệm 127.0.0.1:55442, DB meal_booking_ver3_20261126, có backup được kiểm tra bằng pg_restore. SQL khớp API tổng quan với 2 người hôm nay, 7 người ngày mai, 18 nhân viên và 11 phòng ban. Flyway xác thực V1–V10.
- Maven 30/30, Vite build và git diff --check đạt. Vite vẫn báo cảnh báo kích thước chunk ExcelJS. Cây backend và migration không đổi.
