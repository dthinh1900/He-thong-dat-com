package com.mealbooking.backend.controller;

import com.mealbooking.backend.application.port.in.RegistrationSettings;
import com.mealbooking.backend.application.port.in.MealReports;
import com.mealbooking.backend.application.port.in.WeeklyMenus;
import com.mealbooking.backend.application.port.in.EmployeeAdministration;
import com.mealbooking.backend.application.port.in.AdminMeals;
import com.mealbooking.backend.dto.request.AdminRequests.*;
import com.mealbooking.backend.application.model.AdminResponses.*;
import com.mealbooking.backend.dto.response.MessageResponse;
import com.mealbooking.backend.security.UserPrincipal;
import com.mealbooking.backend.adapter.export.ReportWorkbook;
import com.mealbooking.backend.domain.error.BusinessException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final EmployeeAdministration people;
    private final AdminMeals meals;
    private final WeeklyMenus menus;
    private final MealReports reports;
    private final RegistrationSettings policy;
    public AdminController(EmployeeAdministration people,AdminMeals meals,WeeklyMenus menus,
            MealReports reports,RegistrationSettings policy) {
        this.people=people;this.meals=meals;this.menus=menus;this.reports=reports;this.policy=policy;
    }
    @GetMapping("/overview") public Overview overview(){return meals.overview();}
    @GetMapping("/kitchen-counts")
    public KitchenCounts kitchenCounts(@RequestParam LocalDate date) {
        return meals.kitchenCounts(date);
    }
    @PostMapping("/kitchen-counts")
    public KitchenCounts recordKitchenCounts(@RequestParam LocalDate date, @AuthenticationPrincipal UserPrincipal actor) {
        return meals.recordKitchenCounts(date, actor.user().getId());
    }
    @GetMapping("/departments") public List<DepartmentRow> departments(){return people.departments();}
    @PostMapping("/departments") @ResponseStatus(HttpStatus.CREATED)
    public DepartmentRow createDepartment(@Valid @RequestBody DepartmentInput input){return people.saveDepartment(null,input.toCommand());}
    @PutMapping("/departments/{id}") public DepartmentRow updateDepartment(@PathVariable Long id,@Valid @RequestBody DepartmentInput input){
        return people.saveDepartment(id,input.toCommand());
    }
    @GetMapping("/employees") public List<EmployeeRow> employees(@RequestParam(required=false) Long departmentId,
            @RequestParam(required=false) String search){return people.employees(departmentId,search);}
    @GetMapping("/employees/{id}") public EmployeeRow employee(@PathVariable Long id){return people.employee(id);}
    @PostMapping("/employees") @ResponseStatus(HttpStatus.CREATED)
    public EmployeeRow createEmployee(@Valid @RequestBody EmployeeInput input,@AuthenticationPrincipal UserPrincipal actor){
        return people.saveEmployee(null,input.toCommand(),actor.user().getId());
    }
    @PutMapping("/employees/{id}") public EmployeeRow updateEmployee(@PathVariable Long id,
            @Valid @RequestBody EmployeeInput input,@AuthenticationPrincipal UserPrincipal actor){
        return people.saveEmployee(id,input.toCommand(),actor.user().getId());
    }
    @PatchMapping("/employees/{id}/active") public EmployeeRow active(@PathVariable Long id,
            @Valid @RequestBody ActiveInput input,@AuthenticationPrincipal UserPrincipal actor){
        return people.setActive(id,input.active(),actor.user().getId());
    }
    @DeleteMapping("/employees/{id}") public MessageResponse retire(@PathVariable Long id,@AuthenticationPrincipal UserPrincipal actor){
        people.retire(id,actor.user().getId());return new MessageResponse("Đã ngừng hoạt động tài khoản; giữ lịch sử.");
    }
    @GetMapping("/registrations") public List<RegistrationRow> registrations(@RequestParam LocalDate date,
            @RequestParam(required=false) Long departmentId){return meals.registrations(date,departmentId);}
    @PutMapping("/registrations/{userId}/{date}") public MessageResponse register(@PathVariable Long userId,
            @PathVariable LocalDate date,@Valid @RequestBody RegistrationInput input,@AuthenticationPrincipal UserPrincipal actor){
        meals.setRegistration(userId,date,input.registered(),actor.user().getId(),input.cancellationReason(),input.withdrawal());return new MessageResponse("Đã cập nhật đăng ký.");
    }
    @GetMapping("/registration-settings") public Settings settings(){return policy.settings();}
    @PutMapping("/registration-settings") public Settings settings(@Valid @RequestBody SettingsInput input){
        Settings result=new Settings(input.cutoff(),input.cancelDeadline());policy.update(result);return result;
    }
    @GetMapping("/meal-prices") public List<PriceRow> prices(){return meals.prices();}
    @PostMapping("/meal-prices") @ResponseStatus(HttpStatus.CREATED)
    public PriceRow price(@Valid @RequestBody PriceInput input,@AuthenticationPrincipal UserPrincipal actor){
        return meals.addPrice(input.toCommand(),actor.user().getId());
    }
    @GetMapping("/menus") public MenuRow menu(@RequestParam(required=false) LocalDate weekStart){return menus.get(weekStart);}
    @PutMapping(value="/menus",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public MenuRow upload(@RequestParam(required=false) LocalDate weekStart,@RequestParam MultipartFile file,
            @AuthenticationPrincipal UserPrincipal actor) {
        if (file.isEmpty() || file.getSize() > WeeklyMenus.MAX_IMAGE_BYTES) {
            throw BusinessException.badRequest("Ảnh phải từ 1 byte đến 5 MB.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw BusinessException.badRequest("Không đọc được ảnh.");
        }
        return menus.upload(weekStart, bytes, actor.user().getId());
    }
    @GetMapping("/reports/month") public Report report(@RequestParam int month,@RequestParam int year){return reports.report(month,year);}
    @GetMapping("/reports/month/export")
    public ResponseEntity<byte[]> export(@RequestParam int month,@RequestParam int year) throws IOException {
        byte[] body=ReportWorkbook.write(reports.report(month,year));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=meal-report-"+year+"-"+month+".xlsx").body(body);
    }
}
