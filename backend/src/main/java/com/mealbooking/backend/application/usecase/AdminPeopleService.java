package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.out.PasswordHasher;
import com.mealbooking.backend.application.port.out.PeopleDirectory;
import com.mealbooking.backend.application.port.out.UserStore;
import com.mealbooking.backend.application.port.out.DepartmentStore;
import com.mealbooking.backend.application.port.in.EmployeeAdministration;
import com.mealbooking.backend.application.model.AdminCommands.*;
import com.mealbooking.backend.application.model.AdminCommands.ImportRow;
import com.mealbooking.backend.application.model.AdminResponses.*;
import com.mealbooking.backend.domain.model.*;
import com.mealbooking.backend.domain.enums.Role;
import com.mealbooking.backend.domain.error.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AdminPeopleService implements EmployeeAdministration {
    private final UserStore users;
    private final DepartmentStore departments;
    private final PasswordHasher encoder;
    private final PeopleDirectory directory;
    public AdminPeopleService(UserStore users, DepartmentStore departments, PasswordHasher encoder, PeopleDirectory directory) {
        this.users=users; this.departments=departments; this.encoder=encoder; this.directory=directory;
    }

    public List<DepartmentRow> departments() { return directory.departments(); }
    public DepartmentRow saveDepartment(Long id, DepartmentInput input) {
        directory.lockDepartmentsForWrite();
        Department d=id==null?new Department():department(id);
        String name=input.name().trim();
        if(departments.existsByNameIgnoreCaseAndIdNot(name,id==null?-1L:id))
            throw BusinessException.conflict("Tên phòng ban đã tồn tại.");
        d.setName(name); d.setDescription(clean(input.description()));
        if(input.active()!=null) d.setActive(input.active());
        d=departments.saveAndFlush(d);
        return new DepartmentRow(d.getId(),d.getName(),d.getDescription(),d.getActive(),users.countByDepartmentId(d.getId()));
    }

    public List<EmployeeRow> employees(Long departmentId,String search) {
        String keyword=search==null?"":search.trim().toLowerCase(Locale.ROOT);
        return users.findAllByOrderByFullNameAscIdAsc().stream()
            .filter(u->departmentId==null||departmentId.equals(u.getDepartment().getId()))
            .filter(u->keyword.isEmpty()||List.of(u.getFullName(),u.getEmployeeCode(),u.getUsername()).stream()
                .anyMatch(s->s.toLowerCase(Locale.ROOT).contains(keyword)))
            .map(AdminPeopleService::row).toList();
    }

    public EmployeeRow employee(Long id) { return row(user(id)); }
    public EmployeeRow saveEmployee(Long id, EmployeeInput input, Long actorId) {
        // Serialize changes to roles/status so concurrent admins cannot remove the last admin.
        directory.lockUsersForWrite();
        return writeEmployee(id, input, actorId);
    }
    public List<EmployeeRow> importEmployees(List<ImportRow> rows, Long actorId) {
        Set<String> codes = new HashSet<>();
        Set<String> usernames = new HashSet<>();
        for (ImportRow row : rows) {
            EmployeeInput employee = row.employee();
            if (!codes.add(employee.code().trim().toLowerCase(Locale.ROOT))
                    || !usernames.add(employee.username().trim().toLowerCase(Locale.ROOT))) {
                throw BusinessException.conflict("Dòng " + row.row() + ": mã hoặc tài khoản trùng trong tệp.");
            }
            if (employee.password() == null || employee.password().isBlank()
                    || employee.password().getBytes(StandardCharsets.UTF_8).length > 72) {
                throw BusinessException.badRequest("Dòng " + row.row() + ": cần mật khẩu ban đầu hợp lệ (tối đa 72 byte UTF-8).");
            }
        }
        directory.lockUsersForWrite();
        List<EmployeeRow> imported = new ArrayList<>();
        for (ImportRow row : rows) {
            try {
                imported.add(writeEmployee(null, row.employee(), actorId));
            } catch (BusinessException error) {
                throw error.withMessage("Dòng " + row.row() + ": " + error.getMessage());
            }
        }
        return imported;
    }
    private EmployeeRow writeEmployee(Long id, EmployeeInput input, Long actorId) {
        User u=id==null?new User():user(id);
        String code=input.code().trim(), username=input.username().trim();
        if(users.existsByEmployeeCodeIgnoreCaseAndIdNot(code,id==null?-1L:id)
                ||users.existsByUsernameIgnoreCaseAndIdNot(username,id==null?-1L:id))
            throw BusinessException.conflict("Mã nhân viên hoặc tên đăng nhập đã tồn tại.");
        Department dept=department(input.departmentId());
        if(!dept.getActive() && (id==null || !dept.getId().equals(u.getDepartment().getId())))
            throw BusinessException.badRequest("Phòng ban đã ngừng hoạt động.");
        boolean active=input.active()!=null?input.active():id==null||u.getActive();
        if(id!=null) protectAdmin(u,actorId,input.role(),active);
        if(id==null && input.password()==null) throw BusinessException.badRequest("Cần mật khẩu ban đầu.");
        if(input.password()!=null) {
            if(input.password().isBlank()||input.password().getBytes(StandardCharsets.UTF_8).length>72)
                throw BusinessException.badRequest("Mật khẩu không hợp lệ (tối đa 72 byte UTF-8).");
            u.setPassword(encoder.encode(input.password()));
        }
        u.setEmployeeCode(code); u.setUsername(username); u.setFullName(input.name().trim());
        u.setEmail(clean(input.email())); u.setPhone(clean(input.phone())); u.setDepartment(dept);
        u.setRole(input.role()); u.setActive(active);
        return row(users.saveAndFlush(u));
    }
    public EmployeeRow setActive(Long id,boolean active,Long actorId) {
        directory.lockUsersForWrite();
        User u=user(id); protectAdmin(u,actorId,u.getRole(),active);
        u.setActive(active); return row(users.saveAndFlush(u));
    }
    // Retire accounts rather than deleting references to tickets, transactions and audit history.
    public void retire(Long id,Long actorId) { setActive(id,false,actorId); }
    private void protectAdmin(User u,Long actorId,Role newRole,boolean active) {
        if(u.getId().equals(actorId) && (!active||newRole!=Role.ADMIN))
            throw BusinessException.conflict("Không thể tự khóa hoặc hạ quyền quản trị.");
        if(u.getRole()==Role.ADMIN && u.getActive() && (!active||newRole!=Role.ADMIN)
                && users.countByRoleAndActiveTrue(Role.ADMIN)<=1)
            throw BusinessException.conflict("Phải giữ ít nhất một Admin hoạt động.");
    }
    private User user(Long id) { return users.findById(id).orElseThrow(()->BusinessException.notFound("Không tìm thấy nhân viên.")); }
    private Department department(Long id) { return departments.findById(id).orElseThrow(()->BusinessException.notFound("Không tìm thấy phòng ban.")); }
    private static String clean(String s) { return s==null||s.isBlank()?null:s.trim(); }
    private static EmployeeRow row(User u) {
        return new EmployeeRow(u.getId(),u.getEmployeeCode(),u.getFullName(),u.getUsername(),
            u.getDepartment().getId(),u.getEmail(),u.getPhone(),u.getRole(),u.getActive());
    }
}
