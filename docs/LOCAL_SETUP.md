# Cài đặt và kết nối trên máy local

## 1. Tải mã và chuẩn bị PostgreSQL

Cài Git, Java 21, Maven 3.9+, Node.js 20.19+ và PostgreSQL 15+. Sao chép từ repo và nhánh mà người quản trị dự án đã xác nhận.

```powershell
git clone --branch <ten-nhanh> https://github.com/<chu-so-huu>/<ten-repo>.git
cd <ten-repo>
```

Khởi động PostgreSQL. Dùng database riêng `meal_booking_db` trên `localhost:5432`. Tạo DB rỗng qua pgAdmin hoặc dùng tài khoản có quyền `CREATEDB` để bootstrap local. Flyway tự chạy các migration lịch sử khi backend mở DB rỗng. Với DB có dữ liệu, theo quy trình trong [hướng dẫn database](../database/README.md).

## 2. Cấu hình riêng cho máy

Chạy tại thư mục gốc trong PowerShell. Các lệnh giữ file cấu hình đã có.

```powershell
New-Item -ItemType Directory -Force backend/config | Out-Null
if (!(Test-Path backend/config/application-local.properties)) {
    Copy-Item backend/src/main/resources/application-local.properties.example backend/config/application-local.properties
}
if (!(Test-Path frontend/.env.local)) {
    Copy-Item frontend/.env.example frontend/.env.local
}
```

- Trong `backend/config/application-local.properties`, điền `spring.datasource.username` và `spring.datasource.password` theo máy. Có thể dùng các biến `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` với file mẫu. Spring Boot đọc file trong `backend/config` khi chạy từ `backend`.
- Với DB mới, bật `app.initial-admin.enabled=true` trong file local và điền các dòng `app.initial-admin.*` trong mẫu bằng thông tin thực tế. Mật khẩu từ 12 ký tự, tối đa 72 byte UTF-8. Sau lần khởi tạo, tắt cấu hình này và xóa mật khẩu khởi tạo khỏi file riêng. Quản trị viên đăng nhập và tạo nhân viên từ giao diện. Nếu để trống `JWT_SECRET`, phiên local hết hiệu lực khi backend khởi động lại.
- Trong `frontend/.env.local`, để trống `VITE_API_URL`. Đặt `VITE_API_PROXY_TARGET` trỏ cổng backend. Mặc định là `8080`. Nếu chọn `8081`, đặt `server.port=8081` và đổi proxy sang `http://localhost:8081` cùng lúc.
- Nếu máy có biến `SPRING_DATASOURCE_URL` hoặc `SPRING_DATASOURCE_HIKARI_READ_ONLY` từ dự án khác, đặt lại trong terminal backend. Khi `SPRING_DATASOURCE_HIKARI_READ_ONLY=true`, backend vẫn có thể đọc lịch nhưng PostgreSQL từ chối ghi, ví dụ lúc bấm **Lưu giá**. Các biến `SPRING_DATASOURCE_USERNAME` và `SPRING_DATASOURCE_PASSWORD` cũng có thể ghi đè file local.

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://127.0.0.1:5432/meal_booking_db'
$env:SPRING_DATASOURCE_HIKARI_READ_ONLY = 'false'
```

Các file cấu hình riêng, override Maven local và dữ liệu thử trong `.local/` nằm ngoài Git.

## 3. Chạy bằng hai terminal

Terminal backend tại `backend`:

```powershell
mvn spring-boot:run
```

Chờ dòng `Started MealBookingApplication`. Mở `http://localhost:8080/api/health` hoặc cổng đã chọn, kết quả phải có `status: UP`.

Terminal frontend tại `frontend`:

```powershell
npm.cmd ci
npm.cmd run dev
```

Mở [http://localhost:5173](http://localhost:5173) và kiểm tra thêm [health qua proxy](http://localhost:5173/api/health). Nếu báo cổng 5173 đang được dùng, kiểm tra cửa sổ Vite cũ. Khởi động lại Vite sau khi sửa biến môi trường.
