package by.vs.erp.employee.controller;

import by.vs.erp.employee.dto.*;
import by.vs.erp.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@PreAuthorize(value = "hasRole('ADMIN')")
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping("/register")
    public ResponseEntity<EmployeeResponseDto> register(@Valid @RequestBody EmployeeRegisterRequest request) {
        EmployeeResponseDto response = employeeService.registerNewEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismissEmployee(@PathVariable Long id) {
        log.info("API ADMIN: Запрос на увольнение сотрудника ID: {}", id);
        employeeService.dismissEmployee(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<Void> updateEmail(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmailRequestDto dto) {
        log.info("API ADMIN: Запрос на смену почты сотрудника ID: {}", id);
        employeeService.updateEmail(id, dto.getNewEmail());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/password-reset")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequestDto dto) {
        log.info("API ADMIN: Запрос на сброс пароля сотрудника ID: {}", id);
        employeeService.resetPassword(id, dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/salary")
    public ResponseEntity<Void> updateSalary(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSalaryRequestDto dto) {
        log.info("API ADMIN: Запрос на изменение ставки сотрудника ID: {}", id);
        employeeService.updateSalaryRate(id, dto.getNewSalaryRatePercent());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<Slice<EmployeeResponseDto>> getByRole(@PathVariable String role,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        log.info("API ADMIN: Получение списка сотрудников по роли: {}", role);
        return ResponseEntity.ok(employeeService.getEmployeesByRole(role.toUpperCase(), pageable));
    }

    @GetMapping("/search/email")
    public ResponseEntity<EmployeeResponseDto> searchByEmail(@RequestParam String email) {
        log.info("API ADMIN: Поиск сотрудника по email: {}", email);
        return ResponseEntity.ok(employeeService.findByEmail(email));
    }

    @GetMapping("/search/name")
    public ResponseEntity<Slice<EmployeeResponseDto>> searchByName(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("API ADMIN: Поиск сотрудников по имени: {} {}", firstName, lastName);
        return ResponseEntity.ok(employeeService.searchByName(firstName, lastName, pageable));
    }
}
