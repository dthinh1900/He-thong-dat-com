# Checklist triển khai

Kiểm thử local trước khi review triển khai. GitHub và hạ tầng triển khai được thao tác sau khi có phê duyệt. Mục này mô tả cấu hình ứng dụng, kiểm tra và rollback.

## Chuẩn bị nhánh phát hành

1. Xác nhận URL repo, chủ sở hữu, nhánh phát hành và quyền truy cập bên deploy. Dùng đúng repo đã được duyệt trong cả Vercel và Render.
2. Review working tree và file staged. `.env.local`, cấu hình Spring/Maven riêng máy, fixture credential, bản sao lưu DB và ảnh kiểm thử phải nằm ngoài commit. Chạy Maven test và Vite build trước khi phát hành.
3. Commit và push nhánh sau khi đã được duyệt. Nếu dịch vụ deploy tự động theo nhánh Git, kiểm tra cấu hình auto-deploy trước khi push.
4. Tên hiển thị trên tab lấy từ `frontend/index.html`. Bản build hiện mang tên “Đặt cơm trưa”. Tên miền cấu hình riêng trong dashboard của hạ tầng.

## PostgreSQL và Backend

1. Chọn PostgreSQL managed có persistence, backup và thời hạn lưu phù hợp, dùng Database riêng và URL kết nối JDBC. Giới hạn truy cập mạng của DB cho Backend, kiểm tra TLS của nhà cung cấp, dung lượng và giới hạn connection pool. Tạo backup và thử khôi phục vào DB riêng trước khi migrate production. [Tài liệu backup và recovery của Render](https://render.com/docs/postgresql-backups).
2. Tạo Render Web Service kiểu Docker với Root Directory `backend`. `Dockerfile` dùng Java 21 và profile `prod`. Thêm `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (ít nhất 32 byte UTF-8) và `FRONTEND_ORIGIN` (origin HTTPS chính xác của FE, không có dấu `/` cuối). Credential và JWT lưu trong cấu hình bí mật của dịch vụ. Nếu chạy JAR ngoài Docker, đặt thêm `SPRING_PROFILES_ACTIVE=prod`. [Docker trên Render](https://render.com/docs/docker).
3. Ứng dụng lắng nghe `PORT` nếu hosting đặt biến này. Cấu hình `/api/health` làm Health Check Path, xem log khi boot để xác nhận Flyway migrate, Hibernate validate và cổng được bind. Với DB mới chưa có người dùng, cấp Admin bằng `INITIAL_ADMIN_ENABLED=true` và các biến `INITIAL_ADMIN_USERNAME`, `INITIAL_ADMIN_EMPLOYEE_CODE`, `INITIAL_ADMIN_FULL_NAME`, `INITIAL_ADMIN_DEPARTMENT`, `INITIAL_ADMIN_PASSWORD`. Sau khi đăng nhập, tắt chế độ khởi tạo và xóa biến mật khẩu trên hosting. [Cổng dịch vụ](https://render.com/docs/web-services#port-binding) và [health check](https://render.com/docs/health-checks).
4. Giữ timezone ứng dụng `Asia/Ho_Chi_Minh`. Khi DB legacy chưa có lịch sử Flyway, dùng quy trình nhận DB trong [tài liệu database](../database/README.md). Mọi thay đổi trên DB đang dùng cần backup và kiểm thử restore. V5–V6 sửa mức giá cũ cho kỳ hiện hành và tương lai, giữ số tiền đã ghi trên vé và yêu cầu mua.

### Ảnh thực đơn

Backend lưu nội dung PNG/JPEG trong `menu_image_content` và phục vụ ảnh qua API sau khi người dùng đăng nhập. Backend và frontend có thể khởi động lại hoặc đổi máy chủ mà vẫn đọc được ảnh nếu kết nối tới cùng PostgreSQL. Lưu trữ bền vững và backup của DB bao gồm cả bảng ảnh.

Ảnh đang ở local chỉ xuất hiện trên môi trường triển khai khi Admin tải lại ảnh từ giao diện đã deploy hoặc khi thực hiện chuyển dữ liệu có duyệt, gồm cả `menu_images` và `menu_image_content`. Nếu dùng Render Postgres, kiểm tra thời hạn backup theo gói và thử restore vào DB riêng. [Tài liệu backup PostgreSQL của Render](https://render.com/docs/postgresql-backups)

## Frontend

1. Root Directory `frontend`, framework Vite, build `npm run build`, output `dist`. `frontend/vercel.json` giữ rewrite cho route SPA khi refresh hoặc truy cập thẳng. [Vite trên Vercel](https://vercel.com/docs/frameworks/frontend/vite).
2. Gán `VITE_API_URL=https://<backend-origin>` lúc build. Chỉ đặt origin, không thêm `/api`. `VITE_*` đi vào mã trình duyệt. Không cấu hình mật khẩu hoặc JWT secret ở FE. Vite proxy chỉ dùng khi chạy dev. Thay đổi biến build cần build lại. [Biến môi trường Vercel](https://vercel.com/docs/environment-variables).
3. CORS Backend hiện chấp nhận một origin cụ thể. Dùng origin được duyệt cho từng môi trường và DB riêng cho preview, nếu cần. Kiểm tra `OPTIONS`, `Authorization`, `Content-Type` và tên file export từ domain thật.

## Smoke test và rollback

- Mở HTTPS Backend `/api/health`, kiểm tra log Flyway và HTTP 200; tại FE refresh các route đăng nhập, lịch và Admin. Xác nhận Network gọi đúng origin và không có CORS error.
- Kiểm tra login, phân quyền, đọc/ghi một vé test đã được duyệt cho môi trường triển khai, báo cáo và tải XLSX. Đối soát một bản ghi qua truy vấn đọc trên đúng DB. Không chạy fixture local trên hạ tầng dùng chung.
- Tải một ảnh tuần bằng Admin, khởi động lại backend rồi đăng nhập từ một thiết bị hoặc phiên khác để mở ảnh qua lịch.
- Ghi mã bản build, Flyway version và định danh backup. Khi xảy ra lỗi, rollback bản FE/BE tương thích. Nếu bắt buộc khôi phục dữ liệu, diễn tập restore vào DB mới trước, xác nhận thời điểm cắt và chỉ chuyển kết nối sau khi có phê duyệt.
- Theo dõi uptime bên ngoài, log lỗi/timeout, sức khỏe DB và thời hạn backup. Để vận hành 24/7 cần plan giữ dịch vụ luôn chạy và vận hành đã được duyệt. Kiểm tra sleep, hạn mức, chi phí, backup và SLA của plan thật trước khi cam kết. [Khuyến nghị uptime của Render](https://render.com/docs/uptime-best-practices).
