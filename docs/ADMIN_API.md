# Admin API — backend-admin

## Setup
Flyway applies V1–V4 to a new database. For an existing manually initialized database, follow [database setup and one-time baseline](../database/README.md). Hibernate validates schema. JWT binds to user ID and username.

All /api/admin endpoints require an ADMIN Bearer token. User endpoints remain authenticated. JSON unless indicated.

| Method | Path | Body/query |
|---|---|---|
| GET | /api/admin/overview | |
| GET | /api/admin/departments | |
| POST | /api/admin/departments | {name,description,active?} |
| PUT | /api/admin/departments/{id} | same |
| GET | /api/admin/employees | departmentId?, search? |
| GET | /api/admin/employees/{id} | |
| GET | /api/admin/employees/{id}/history | month, year |
| POST | /api/admin/employees | {code,name,username,email?,phone?,departmentId,role,password,active?} |
| POST | /api/admin/employees/import | {rows:[{row,employee}]} with the employee creation fields |
| PUT | /api/admin/employees/{id} | same, password optional |
| PATCH | /api/admin/employees/{id}/active | {active} |
| DELETE | /api/admin/employees/{id} | soft retire (active=false) |
| GET | /api/admin/registrations | date=2026-11-19, departmentId? |
| PUT | /api/admin/registrations/{userId}/{date} | {registered:true/false, cancellationReason?, withdrawal?} |
| PATCH | /api/admin/meal-tickets/{id}/cancel | withdraw an editable booking or cancel a locked future ticket. Optional JSON fields are reason and withdrawal (expected intent) |
| GET | /api/admin/purchases | |
| PATCH | /api/admin/purchases/{id} | {decision:"APPROVED" or "REJECTED"} |
| GET | /api/admin/exchange-transactions | completed ticket transfers |
| GET | /api/admin/exchange-audit | listing, matching and cancellation events |
| GET/PUT | /api/admin/registration-settings | {cutoff:"14:45",cancelDeadline:"14:45"} for PUT |
| GET/POST | /api/admin/meal-prices | POST nhận `ticketType`, `price` và `effectiveFrom` từ hôm nay. Cùng ngày hiệu lực thì cập nhật; mốc xen giữa giữ các kỳ đã lên lịch sau đó. |
| GET | /api/admin/menus | weekStart? (ISO date) |
| PUT | /api/admin/menus | multipart: file, weekStart? |
| GET | /api/menus | authenticated User or Admin, weekStart? |
| GET | /api/menus/{weekStart}/image | authenticated image bytes (fetch with Bearer, create object URL in FE) |
| GET | /api/admin/reports/month | month=11&year=2026 |
| GET | /api/admin/reports/month/export | month=11&year=2026; binary XLSX |
| GET | /api/admin/reports/detail | from and to (inclusive ISO dates), up to 366 days |
| GET | /api/admin/reports/range/export | from and to; binary XLSX with daily and department tabs |

Employee creation and import require an initial password. Import creates all rows in one transaction. DELETE retires the account with `active=false` and preserves its history. Admin cannot lock or demote themselves or the last active admin. Sign in again after a username change.

The Admin registration table changes a registration-based ticket. The exact-ticket endpoint also cancels available purchased or transferred tickets. Admin cancellation starts tomorrow. A reserved ticket must be released from its sale first. PNG/JPEG bytes up to 5 MB are stored in PostgreSQL. Missing menus return 404. Reports count current-owner tickets in AVAILABLE, LISTED_FOR_EXCHANGE or USED with stored amounts. A completed transfer is shown in history and counted once in current-owner totals. Reports group by the owner's current department. Meal price changes start in the future and retain earlier ticket prices.

## Manual acceptance checks
- USER token -> GET /api/admin/overview -> 403; missing token -> 401; ADMIN -> 200.
- Create department and user; duplicate code/name -> 409; invalid email or missing password -> 400.
- Lock user -> login fails and existing token rejected; self-lock -> 409.
- Register a future weekday for a user, cancel and repeat; inspect audit_logs.actor_id and meal_tickets.cancelled_by.
- Verify user cutoff and the Admin cancellation exception for future dates.
- Upload valid PNG, GET image as User, replace; reject oversized or invalid image.
- Add future price; verify old ticket snapshot is unchanged and report totals sum ticket price.
- Export month; open XLSX and compare totals with JSON and database.
