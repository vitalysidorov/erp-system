package by.vs.erp.employee.service;

import by.vs.erp.employee.dto.EmployeeRegisterRequest;
import by.vs.erp.employee.dto.EmployeeResponseDto;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    @DisplayName("Успешная регистрация нового сотрудника с шифрованием пароля")
    void registerNewEmployee_Success() {
        EmployeeRegisterRequest request = new EmployeeRegisterRequest();
        request.setEmail("new@erp.by");
        request.setPassword("rawPassword");
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setRole("MECHANIC");
        request.setSalaryRatePercent(new BigDecimal("40.00"));

        Employee savedEmployee = new Employee();
        savedEmployee.setId(10L);
        savedEmployee.setEmail("new@erp.by");
        savedEmployee.setPassword("encodedPassword");
        savedEmployee.setFirstName("Иван");
        savedEmployee.setLastName("Иванов");
        savedEmployee.setRole("MECHANIC");
        savedEmployee.setSalaryRatePercent(new BigDecimal("40.00"));
        savedEmployee.setIsActive(true);

        when(employeeRepository.existsByEmail("new@erp.by")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        EmployeeResponseDto response = employeeService.registerNewEmployee(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("new@erp.by", response.getEmail());
        assertTrue(response.getIsActive());
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("Регистрация отклонена, если email уже занят другим сотрудником")
    void registerNewEmployee_ThrowsIllegalArgumentException_WhenEmailExists() {
        EmployeeRegisterRequest request = new EmployeeRegisterRequest();
        request.setEmail("occupied@erp.by");

        when(employeeRepository.existsByEmail("occupied@erp.by")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> employeeService.registerNewEmployee(request));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успешное увольнение сотрудника с деактивацией и отзывом сессии")
    void dismissEmployee_Success() {
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setIsActive(true);
        employee.setRefreshToken("active-refresh-token");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        employeeService.dismissEmployee(employeeId);

        assertFalse(employee.getIsActive(), "Флаг активности должен измениться на false");
        assertNull(employee.getRefreshToken(), "Refresh-токен должен быть сброшен");
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("Смена почты: успешное обновление на свободный email")
    void updateEmail_Success() {
        Long employeeId = 2L;
        String newEmail = "updated@erp.by";
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setEmail("old@erp.by");

        when(employeeRepository.existsByEmail(newEmail)).thenReturn(false);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        employeeService.updateEmail(employeeId, newEmail);

        assertEquals(newEmail, employee.getEmail());
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("Смена почты отклонена, если целевой email уже используется")
    void updateEmail_ThrowsIllegalStateException_WhenNewEmailOccupied() {
        Long employeeId = 2L;
        String busyEmail = "busy@erp.by";

        when(employeeRepository.existsByEmail(busyEmail)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> employeeService.updateEmail(employeeId, busyEmail));
        verify(employeeRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Успешный административный сброс пароля и отзыв текущего токена сессии")
    void resetPassword_Success() {
        Long employeeId = 3L;
        String newPassword = "newRawPassword";
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setPassword("oldEncodedPassword");
        employee.setRefreshToken("current-session-token");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");

        employeeService.resetPassword(employeeId, newPassword);

        assertEquals("newEncodedPassword", employee.getPassword());
        assertNull(employee.getRefreshToken(), "Сессия должна инвалидироваться");
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("Успешное изменение процентной ставки заработной платы")
    void updateSalaryRate_Success() {
        Long employeeId = 4L;
        BigDecimal newRate = new BigDecimal("65.50");
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setSalaryRatePercent(new BigDecimal("50.00"));

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        employeeService.updateSalaryRate(employeeId, newRate);

        assertEquals(newRate, employee.getSalaryRatePercent());
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("Поиск по email: успешное получение сущности и маппинг в DTO")
    void findByEmail_Success() {
        String email = "find@erp.by";
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmail(email);
        employee.setFirstName("Олег");
        employee.setLastName("Петров");

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        EmployeeResponseDto result = employeeService.findByEmail(email);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Олег", result.getFirstName());
    }

    @Test
    @DisplayName("Поиск сотрудников по имени и фамилии возвращает Slice DTO элементов")
    void searchByName_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Employee employee = new Employee();
        employee.setId(6L);
        employee.setFirstName("Анна");
        employee.setLastName("Сидорова");

        Slice<Employee> employeeSlice = new SliceImpl<>(List.of(employee), pageable, false);
        when(employeeRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("Анна", "Сидорова", pageable))
                .thenReturn(employeeSlice);

        Slice<EmployeeResponseDto> result = employeeService.searchByName("Анна", "Сидорова", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Анна", result.getContent().get(0).getFirstName());
    }
}
