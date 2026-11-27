# Kế hoạch triển khai local

Kiến trúc giữ React JSX/Vite → service → REST JWT → Spring Boot Controller/Service → JPA/JdbcTemplate → PostgreSQL. Dùng schema Flyway hiện có. Chạy kiểm thử API trên PostgreSQL riêng.

| Slice | Mapping | Xác minh |
|---|---|---|
| P0 | README, routes, preview, mock, controllers và DB scripts → kiểm kê phụ thuộc | Baseline Maven/FE. Bảng cleanup ở decision log. |
| P1 | Local config → Flyway validate → fixture riêng trong PostgreSQL | Đọc thông tin đích và backup trước seed. Seed lặp lại không đổi account, giá hay fixture hiện hữu. |
| P2 | UserExtras/Admin tickets → ticket service → /api/ticket-* và /api/admin/* → purchase/exchange service → repo → tickets/audit | Unit, contract và live readback. Lock theo thứ tự ổn định, rollback transaction. |
| P2 | Registration/Department → mealService → /api/meal-registrations, /api/departments → registration/roster → tickets/people | Đối chiếu cutoff và chủ sở hữu, giá theo ngày và lịch sử. |
| P3 | AdminPage/ReportDashboard → adminService → /api/admin → people, meals, menus, reports, Excel → PostgreSQL | Cùng contract Mock/API. Kiểm tra drill-down, people import, báo cáo tổng hợp và quyền. |
| P4 | Vite proxy + build origin, Docker, SPA, config local → health, browser và regression | Kiểm tra hai data mode, desktop/mobile, đối soát SQL. README/deploy hướng dẫn rollback bằng version trước và backup, không thực hiện deployment. |

Schema bổ sung chỉ sau khi baseline/backup tại DB local, giữ các hàng cũ. Khôi phục về phiên bản app trước khi cần rollback ứng dụng, giữ cột bổ sung để bảo toàn dữ liệu. Restore từ backup được xem xét riêng và cần phê duyệt nếu tác động dữ liệu.
