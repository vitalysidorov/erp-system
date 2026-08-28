package by.vs.erp.employee.repository;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.employee.entity.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("Должен успешно найти сотрудника по email")
    void shouldFindEmployeeByEmail() {
        Employee employee = createSampleEmployee("test@erp.by", "MECHANIC");
        employeeRepository.save(employee);

        Optional<Employee> found = employeeRepository.findByEmail("test@erp.by");

        assertTrue(found.isPresent());
        assertEquals("Иван", found.get().getFirstName());
    }

    @Test
    @DisplayName("Должен подтвердить существование email в базе")
    void shouldReturnTrueWhenEmailExists() {
        Employee employee = createSampleEmployee("exists@erp.by", "MANAGER");
        employeeRepository.save(employee);

        boolean exists = employeeRepository.existsByEmail("exists@erp.by");
        boolean notExists = employeeRepository.existsByEmail("absent@erp.by");

        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    @DisplayName("Должен выбросить исключение при попытке дублирования уникального email")
    void shouldThrowExceptionWhenEmailIsNotUnique() {
        Employee employee1 = createSampleEmployee("duplicate@erp.by", "MASTER");
        employeeRepository.saveAndFlush(employee1);

        Employee employee2 = createSampleEmployee("duplicate@erp.by", "MECHANIC");
        employee2.setLastName("Петров");

        assertThrows(DataIntegrityViolationException.class, () -> {
            employeeRepository.saveAndFlush(employee2);
        }, "Уникальный ключ по полю email должен вызвать откат транзакции");
    }

    @Test
    @DisplayName("Должен вернуть Slice сотрудников с фильтрацией по роли")
    void shouldReturnSliceByRole() {
        employeeRepository.save(createSampleEmployee("emp1@erp.by", "MASTER"));
        employeeRepository.save(createSampleEmployee("emp2@erp.by", "MASTER"));
        employeeRepository.save(createSampleEmployee("emp3@erp.by", "MECHANIC"));

        Slice<Employee> masters = employeeRepository.findByRole("MASTER", PageRequest.of(0, 10));

        assertEquals(2, masters.getContent().size());
        assertTrue(masters.getContent().stream().allMatch(e -> e.getRole().equals("MASTER")));
    }

    @Test
    @DisplayName("Должен найти сотрудников по части имени и фамилии без учета регистра")
    void shouldSearchByNameIgnoreCaseAndContaining() {
        Employee employee = createSampleEmployee("search@erp.by", "MANAGER");
        employee.setFirstName("Александр");
        employee.setLastName("Смирнов");
        employeeRepository.save(employee);

        Slice<Employee> result = employeeRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
                "сандр", "МИРН", PageRequest.of(0, 10)
        );

        assertFalse(result.isEmpty());
        assertEquals("Александр", result.getContent().get(0).getFirstName());
    }

    private Employee createSampleEmployee(String email, String role) {
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPassword("hashed_password_string");
        employee.setFirstName("Иван");
        employee.setLastName("Иванов");
        employee.setRole(role);
        employee.setSalaryRatePercent(new BigDecimal("50.00"));
        employee.setIsActive(true);
        return employee;
    }
}
