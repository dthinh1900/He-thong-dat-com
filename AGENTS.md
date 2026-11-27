# Làm việc trong repository này

- Đọc [spec](spec.md), [plan](plan.md), [tasks](tasks.md) và [quyết định](docs/decisions.md) trước khi sửa nghiệp vụ. Cập nhật spec trước khi triển khai delta, viết Given/When/Then và ghi kết quả thật ở [kiểm thử](docs/verification.md).
- Giữ working tree. Chỉ thao tác DB local/test riêng đã xác minh và có backup. Giữ migration cũ và dữ liệu hiện hữu. Không commit, push, pull, merge, reset, deploy hay thay secret thật khi chưa được người dùng ủy quyền.
- Frontend dùng API với JWT và PostgreSQL; lỗi kết nối hiện rõ. Truy vết consumer trước cleanup.
- Gate cuối: Maven test, Vite build, API/Browser smoke trên PostgreSQL riêng, đối soát SQL, kiểm tra diff. Chỉ đánh dấu verified khi có bằng chứng.
