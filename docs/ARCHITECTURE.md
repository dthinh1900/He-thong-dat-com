# Kiến trúc triển khai hiện tại

```
React SPA (Vite)
  ├── AuthContext / apiClient
  ├── LoginPage
  ├── RegistrationPage ───────── REST /api/meal-registrations
  └── ProfilePage ───────────── REST /api/users/me
                 │
                 ▼
Spring Boot 3
  ├── Security: BCrypt + JWT Bearer
  ├── Controller → Port đầu vào → Use case / Domain
  ├── Use case → Port đầu ra ← Adapter JPA / JDBC
  └── Flyway migration + Hibernate validate
                 │
                 ▼
PostgreSQL (meal_booking_db)
```

## Phạm vi đã kết nối

- Đăng nhập bằng username/password, tạo JWT.
- Lấy/cập nhật hồ sơ cá nhân của tài khoản đang đăng nhập.
- Lấy lịch đăng ký tháng, lấy giá theo từng ngày, thêm/hủy vé và ghi AuditLog.
- User mua thêm vé, đăng bán/nhận vé, xem thực đơn và tra cứu phòng qua API.
- Admin quản lý tài khoản và phòng, duyệt mua thêm, xem nhật ký chuyển nhượng, cấu hình thực đơn/giá và xuất báo cáo XLSX.
- Quyền truy cập và chủ sở hữu được xác định tại backend qua JWT. Frontend lấy dữ liệu và gửi thao tác qua API.

## Tìm mã theo chức năng

| Thư mục | Nội dung |
|---|---|
| `frontend/src/pages`, `frontend/src/routes` | Màn hình chính và đường dẫn |
| `frontend/src/features/registration` | Ghi chú suất thứ Sáu, tra cứu phòng và hộp thoại thực đơn |
| `frontend/src/features/tickets` | Mua thêm, chợ vé, duyệt mua và lịch sử giao dịch |
| `frontend/src/features/admin` | Tổng quan, nhân viên và phòng ban, thực đơn và giá, đăng ký và quy định. `AdminPage` giữ khung và điều hướng giữa các mục. |
| `frontend/src/features/reports` | Biểu đồ và giao diện báo cáo |
| `frontend/src/features/profile` | Đối soát chi phí cá nhân |
| `frontend/src/services`, `frontend/src/components` | Gọi REST và thành phần giao diện dùng chung |
| `backend/src/main/java/com/mealbooking/backend` | Controller, application, domain, adapter và cấu hình |
| `backend/src/main/resources/db/migration` | Tệp Flyway dùng cho cả local và Neon |

## Clean Architecture trong Spring Boot

| Lớp | Vị trí và trách nhiệm |
|---|---|
| Domain | `domain/model` và `domain/enums` giữ mô hình nghiệp vụ. `domain/registration` giữ quy tắc ngày đăng ký và giờ chốt. `domain/error` biểu diễn lỗi nghiệp vụ. |
| Application | `application/port/in` định nghĩa các thao tác từ đăng nhập, hồ sơ và đối soát đến lịch, tra cứu, giá, mua thêm, chuyển vé, nhân sự, thực đơn và báo cáo. `application/usecase` điều phối nghiệp vụ qua các interface trong `application/port/out`. Dữ liệu trao đổi của use case nằm ở `application/model`. |
| Adapter HTTP | `controller` xác thực dữ liệu đầu vào bằng các request trong `dto/request` rồi chuyển chúng thành lệnh nghiệp vụ. Lớp web ánh xạ lỗi nghiệp vụ sang HTTP. |
| Adapter hạ tầng | `adapter/persistence` và `adapter/persistence/jpa` triển khai truy vấn và lưu dữ liệu. `adapter/security` kết nối xác thực và JWT. `adapter/media` đọc định dạng ảnh và `adapter/export` tạo Excel. |
| Cấu hình | `config/ApplicationUseCases` và `config/MealBookingUseCases` nối các lớp qua constructor. Cấu hình Spring bọc transaction quanh port đầu vào, gồm giao dịch chỉ đọc và `REPEATABLE READ` cho báo cáo. |

Ví dụ, khi duyệt đơn mua thêm, `AdminTicketController` gọi `TicketOperations`. Use case kiểm tra trạng thái đơn và gọi các port lưu vé, đơn mua và nhật ký. Adapter JPA thực hiện khóa và lưu dữ liệu trong cùng giao dịch. Để đổi cách lưu, triển khai lại port tương ứng và nối adapter mới ở cấu hình. [Clean Architecture with Spring Boot](https://www.baeldung.com/spring-boot-clean-architecture)

Frontend đặt `PurchasePage` và `ExchangePage` riêng trong `features/tickets`, dùng chung cách hiển thị vé. Biểu đồ tổng quan, hộp chi tiết và xuất Excel nằm trong các mô-đun riêng thuộc `features/reports`.

## Database

- V1 tạo các bảng nghiệp vụ gốc, V2 tạo cấu hình đăng ký, V3 lưu nội dung ảnh, V4 thêm liên kết giá mua và nhóm bài bán.
- V5–V7 điều chỉnh giá theo kỳ. V8 lưu số suất đã báo bếp.
- Nguồn migration duy nhất: `backend/src/main/resources/db/migration`; xem [hướng dẫn DB](../database/README.md).
- Flyway quản lý schema và migration. Hibernate kiểm tra schema bằng cấu hình `validate`, dùng ánh xạ JPA tại `META-INF/orm.xml`.
- `MealTicket.price` lưu giá snapshot; giá cũ không đổi khi cấu hình giá mới.
- Mỗi `MealRegistration` duy nhất theo user/tháng/năm; mỗi ngày một vé từ phiếu đăng ký đó.
- Không xóa cứng các bản ghi vé/giao dịch để giữ lịch sử.

## Quy tắc cần chú ý

- Hạn đăng ký/hủy 14:45 ngày liền trước được kiểm tra tại Backend theo múi giờ `Asia/Ho_Chi_Minh`.
- Mặc định khóa thứ Bảy/Chủ nhật với `EXCLUDE_WEEKENDS=true`.
- Lịch tải bảng giá hiệu lực bằng một truy vấn có tham số. Biên hiệu lực và tổng giá được xét theo từng ngày. Giá vé đã phát hành dùng snapshot.

## Quyền và truy vấn

- Các truy vấn nhận dữ liệu người dùng qua tham số JPA/JDBC. Luồng duyệt và chuyển nhượng dùng transaction và khóa hàng. Các migration đã áp dụng được giữ nguyên.
- Stored function có thể hữu ích cho phân quyền DB hoặc phép tổng hợp dùng chung. Chỉ nên chuyển truy vấn đã đo trên dữ liệu đại diện, với role DB có quyền tối thiểu. Đối với lịch tháng, tối ưu hiện tại giảm số lượt truy vấn giá từ 56–62 xuống 1. Xem [đối soát](verification.md) và [PostgreSQL EXPLAIN](https://www.postgresql.org/docs/18/sql-explain.html).
