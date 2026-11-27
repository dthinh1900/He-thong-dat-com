# Ghi chú tích hợp phiên bản

> Ghi chú lịch sử trước khi kết nối đầy đủ. Xem [kiến trúc hiện tại](ARCHITECTURE.md) và [kiểm thử](verification.md) để biết trạng thái mới.

## Nguồn mã
- Frontend: bản tính năng mới, được đối chiếu với frontend.zip mới nhất của người dùng.
- Backend: lấy từ backend(4).zip mới nhất của người dùng.
- Backend không bị thay đổi logic trong quá trình ghép.

## Kết nối hiện tại

Frontend gọi API Spring Boot qua Vite proxy khi chạy local hoặc qua `VITE_API_URL` khi tách máy chủ. Backend kiểm tra JWT và đọc ghi PostgreSQL. Xem [giao diện Admin](ADMIN_FE_INTEGRATION.md) và [hướng dẫn triển khai](DEPLOYMENT.md).
