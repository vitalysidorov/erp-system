package by.vs.erp.employee.service;

import by.vs.erp.employee.dto.EmployeeRegisterRequest;
import by.vs.erp.employee.dto.EmployeeResponseDto;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmployeeResponseDto registerNewEmployee(EmployeeRegisterRequest request) {
        log.info("Попытка регистрации нового сотрудника с email: {}", request.getEmail());

        if (employeeRepository.existsByEmail(request.getEmail())) {
            log.warn("Регистрация отклонена: email {} уже занят", request.getEmail());
            throw new IllegalArgumentException("Сотрудник с таким email уже зарегистрирован");
        }

        Employee employee = new Employee();
        employee.setEmail(request.getEmail());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setLastName(request.getLastName());
        employee.setFirstName(request.getFirstName());
        employee.setRole(request.getRole());
        employee.setSalaryRatePercent(request.getSalaryRatePercent());
        employee.setIsActive(true);

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Сотрудник успешно создан с ID: {}", savedEmployee.getId());

        return mapToResponse(savedEmployee);
    }

    private EmployeeResponseDto mapToResponse(Employee employee) {
        return EmployeeResponseDto.builder()
                .id(employee.getId())
                .email(employee.getEmail())
                .lastName(employee.getLastName())
                .firstName(employee.getFirstName())
                .role(employee.getRole())
                .salaryRatePercent(employee.getSalaryRatePercent())
                .isActive(employee.getIsActive())
                .build();
    }

    @Transactional
    public void dismissEmployee(Long id) {
        log.info("Сервис: Увольнение сотрудника с ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        employee.setIsActive(false);
        employee.setRefreshToken(null);
        employeeRepository.save(employee);
    }

    @Transactional
    public void updateEmail(Long id, String newEmail) {
        log.info("Сервис: Смена email для сотрудника ID: {} на {}", id, newEmail);
        if (employeeRepository.existsByEmail(newEmail)) {
            throw new IllegalStateException("Сотрудник с таким email уже зарегистрирован в системе");
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        employee.setEmail(newEmail);
        employeeRepository.save(employee);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        log.info("Сервис: Административный сброс пароля для сотрудника ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        employee.setPassword(passwordEncoder.encode(newPassword));
        employee.setRefreshToken(null); // инвалидируем текущую сессию безопасности
        employeeRepository.save(employee);
    }

    @Transactional
    public void updateSalaryRate(Long id, java.math.BigDecimal newRate) {
        log.info("Сервис: Изменение процентной ставки сотрудника ID: {} на {}%", id, newRate);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        employee.setSalaryRatePercent(newRate);
        employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public Slice<EmployeeResponseDto> getEmployeesByRole(String role, Pageable pageable) {
        return employeeRepository.findByRole(role, pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDto findByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .map(this::convertToDto)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник с таким email не найден"));
    }

    @Transactional(readOnly = true)
    public Slice<EmployeeResponseDto> searchByName(String firstName, String lastName, Pageable pageable) {
        return employeeRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(firstName, lastName, pageable)
                .map(this::convertToDto);
    }

    private EmployeeResponseDto convertToDto(Employee employee) {
        return EmployeeResponseDto.builder()
                .id(employee.getId())
                .email(employee.getEmail())
                .lastName(employee.getLastName())
                .firstName(employee.getFirstName())
                .role(employee.getRole())
                .salaryRatePercent(employee.getSalaryRatePercent())
                .isActive(employee.getIsActive())
                .build();
    }
}
