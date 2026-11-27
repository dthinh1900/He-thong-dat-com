# Quản lý Database — PostgreSQL + Flyway

Nguồn migration schema Flyway chính thức duy nhất: [backend/src/main/resources/db/migration](../backend/src/main/resources/db/migration).
Không chạy thủ công migration Flyway, không dùng Hibernate để cập nhật schema. Riêng V0 là bootstrap cấp database do Spring Boot chạy tự động trước Flyway trên database quản trị `postgres`.

| Version | File | Mục đích |
|---|---|---|
| 0 (bootstrap, tự động) | db/bootstrap/V0__create_database.sql | Tạo meal_booking_db nếu chưa tồn tại; không thuộc lịch sử Flyway |
| 1 | V1__init_database.sql | Tạo 10 bảng nghiệp vụ gốc, indexes, trigger kiểm tra khoảng hiệu lực giá |
| 2 | V2__create_registration_settings.sql | Tạo bảng cấu hình hạn đăng ký/hủy, mặc định 14:45 |
| 3 | V3__create_menu_image_content.sql | Tạo bảng lưu nội dung ảnh thực đơn |
| 4 | V4__link_purchase_prices_and_sale_groups.sql | Liên kết giá mua thêm và gom các vé thật trong cùng một bài bán |
| 5 | V5__correct_standard_meal_prices.sql | Áp dụng giá chuẩn 25.000đ/40.000đ cho kỳ hiện hành |
| 6 | V6__correct_scheduled_meal_prices.sql | Chỉnh các mức cũ còn hiệu lực hoặc đã lên lịch, giữ giá lịch sử và số tiền đã ghi trên vé |
| 7 | V7__correct_active_registration_ticket_prices.sql | Sửa vé đăng ký tháng còn khả dụng, chưa chuyển nhượng, của ngày hiện tại và tương lai khi giá lưu còn là mức cũ |
| 8 | V8__record_kitchen_meal_counts.sql | Lưu số suất đã báo bếp theo ngày và loại suất để đối chiếu phát sinh |

Local dùng PostgreSQL trên máy, kiểm thử dùng bản sao riêng, Production dùng PostgreSQL Neon qua Render. Cả ba môi trường dùng cùng bộ migration trong JAR. Mỗi môi trường chọn URL và thông tin kết nối bằng profile Spring hoặc biến môi trường tương ứng. Ở Production, chỉ Render kết nối PostgreSQL.

## 1. Database mới (rỗng)

1. Đảm bảo PostgreSQL local đã chạy.
2. Tạo `backend/config/application-local.properties` từ mẫu trong `backend/src/main/resources` nếu chưa có. Điền thông tin kết nối và cấu hình cấp quản trị viên theo [hướng dẫn chạy local](../docs/LOCAL_SETUP.md). File local nằm ngoài JAR và được Git bỏ qua.
3. Từ thư mục `backend` chạy `mvn spring-boot:run`. Không cần psql cho bootstrap.
4. Spring Boot chạy LocalDatabaseBootstrap trước khi Hikari/Flyway/JPA khởi tạo:
   - V0 thử kết nối đúng DB local `meal_booking_db`. Nếu DB đã tồn tại, bỏ qua bước tạo.
   - Nếu PostgreSQL trả SQLSTATE 3D000 (DB chưa tồn tại), kết nối database quản trị `postgres` trên cùng máy/cổng và chạy `db/bootstrap/V0__create_database.sql` với autocommit.
   - Flyway chạy V1 đến migration mới nhất trước Hibernate validate.
5. Bảng public.flyway_schema_history do Flyway quản lý. Không tự tạo hoặc sửa bảng này.

V0 chỉ tự chạy khi profile local được sử dụng, URL có dạng `jdbc:postgresql://localhost:5432/meal_booking_db` (cũng hỗ trợ 127.0.0.1, [::1] và cổng khác; không có query parameters), và bootstrap được bật. Tài khoản cần quyền CREATEDB và truy cập database postgres khi cần tạo DB mới. Không tự tạo DB từ xa, DB khác, hoặc URL tùy chỉnh; không ghi đè cấu hình read-only. Tắt bằng `app.database.bootstrap.enabled=false` trong file local hoặc `DB_BOOTSTRAP_ENABLED=false` khi không muốn tự tạo. V0 không nằm trong flyway_schema_history (baseline version 0 của DB cũ là khái niệm khác).

Migration tạo schema và cấu hình giá chuẩn. Để cấp quản trị viên đầu tiên, xem [hướng dẫn chạy local](../docs/LOCAL_SETUP.md).

## 2. Database cũ đã chạy SQL thủ công (chỉ làm một lần)

V1–V3 hiện được thiết kế bổ sung bảng thiếu, giữ dữ liệu cũ và không ghi đè cấu hình 14:45 nếu đã được tùy chỉnh.
Vì từng có DB bị thiếu audit_logs, chọn baseline version 0 thay vì 1; tất cả V1–V3 vẫn chạy.
Mặc định baseline-on-migrate=false để tránh tự động tiếp nhận nhầm DB.

- Dừng backend đang chạy và backup database (pgAdmin > Backup).
- Trong Query Tool của đúng DB ứng dụng, kiểm tra:

```sql
SELECT current_database(), current_schema();
SELECT schemaname, tablename FROM pg_tables
WHERE schemaname = 'public' ORDER BY tablename;
SELECT to_regclass('public.flyway_schema_history') AS flyway_history;
```

Chỉ tiếp tục nếu DB đúng là DB đặt cơm của bạn, các bảng hiện tại dùng schema public, và flyway_history trả về NULL. Nếu đang sử dụng schema khác, DB không đúng, hoặc đã có lịch sử Flyway với phiên bản/checksum khác, dừng và kiểm tra; không xóa lịch sử, không tự chạy repair hoặc baseline.

Từ PowerShell tại thư mục backend, bật baseline tạm thời trong phiên hiện tại:

```powershell
$env:SPRING_FLYWAY_BASELINE_ON_MIGRATE = "true"
$env:SPRING_FLYWAY_BASELINE_VERSION = "0"
try {
    mvn clean spring-boot:run
} finally {
    Remove-Item Env:SPRING_FLYWAY_BASELINE_ON_MIGRATE -ErrorAction SilentlyContinue
    Remove-Item Env:SPRING_FLYWAY_BASELINE_VERSION -ErrorAction SilentlyContinue
}
```

Sau khi khởi động thành công, Ctrl+C để dừng nếu muốn chạy lại bình thường. Các biến baseline chỉ dùng lần chuyển đổi đầu; không lưu chúng vào file cấu hình dùng chung hoặc bật vĩnh viễn trên Render.

Nếu DB đã được Flyway quản lý thì khởi động Backend bình thường. Flyway xác minh checksum migration cũ và tự áp dụng migration mới.

Lưu ý: CREATE TABLE IF NOT EXISTS không thay đổi cấu trúc bảng đã có. Nếu Hibernate báo thiếu cột, sai kiểu, hoặc migration báo thiếu ràng buộc, cần so sánh schema cũ và xử lý có chủ đích; không dùng ddl-auto=update/clean để che lỗi. Không dùng script maintenance cho DB đã được Flyway quản lý.

## 3. Kiểm tra

```sql
SELECT installed_rank, version, description, type, success
FROM public.flyway_schema_history ORDER BY installed_rank;

SELECT tablename FROM pg_tables
WHERE schemaname = 'public' ORDER BY tablename;

SELECT id, cutoff, cancel_deadline FROM public.registration_settings;
```

Mong đợi các migration của nhánh đang chạy đều thành công. `ver2` và `main` sau khi tích hợp `ver2` có V1–V7. `ver3` bổ sung V8. DB tiếp nhận theo quy trình cũ có thêm baseline version 0. Khởi động lại để xác nhận Flyway validate đúng và dữ liệu cũ vẫn đọc được.

## 4. Quy tắc nhóm

- Cú pháp: `V<version>__<verb>_<description>.sql` (hai dấu gạch dưới, tiếng Anh snake_case).
- Kiểm tra phiên bản lớn nhất trên main và PR đang mở trước khi chọn version tiếp theo. Không trùng version giữa các nhánh.
- Sau khi migration được áp dụng, không đổi tên hoặc sửa nội dung/checksum; thêm migration mới để sửa.
- Không kèm BEGIN/COMMIT trong migration thông thường; Flyway quản lý transaction PostgreSQL.
- `ddl-auto=validate`, `clean-disabled=true`, không bỏ qua validation. Không commit dữ liệu cá nhân/secret.
- Review và backup trước khi chạy trên môi trường dùng chung. Không deploy tự động thay đổi DB production nếu chưa chuẩn bị rollout.

## 5. Kết nối migration

Flyway dùng kết nối JDBC riêng, lấy URL, username và password từ cấu hình datasource hiệu lực (bao gồm file local). Không cần khai báo lại mật khẩu cho Flyway. Phiên kết nối migration được yêu cầu READ WRITE; cấu hình không đổi quyền tài khoản, không đổi mặc định của toàn server và không biến replica thành primary. Hikari phục vụ JPA riêng, mặc định read-only=false. Nếu server là replica hoặc tài khoản không có quyền DDL, migration vẫn dừng an toàn. Baseline DB cũ vẫn phải thực hiện có chủ đích như mục 2.

## 6. Maintenance

Snapshot SQL phục hồi cũ tại thời điểm V3 nằm trong lịch sử Git. Nếu cần tra cứu, dùng `git show 9a444ac:database/maintenance/repair_missing_tables.sql`. Quy trình hiện tại dùng Flyway và backup PostgreSQL để kiểm tra hoặc phục hồi.
