package by.vs.erp.employee.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.employee.dto.*;
import by.vs.erp.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnableMethodSecurity
class EmployeeControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Успешная регистрация нового сотрудника администратором")
    void shouldRegisterEmployeeWhenUserIsAdmin() throws Exception {
        EmployeeRegisterRequest request = new EmployeeRegisterRequest();
        request.setEmail("mechanic@erp.by");
        request.setPassword("password123");
        request.setFirstName("Петр");
        request.setLastName("Петров");
        request.setRole("MECHANIC");
        request.setSalaryRatePercent(new BigDecimal("45.00"));

        EmployeeResponseDto responseDto = EmployeeResponseDto.builder()
                .id(1L)
                .email("mechanic@erp.by")
                .firstName("Петр")
                .lastName("Петров")
                .role("MECHANIC")
                .salaryRatePercent(new BigDecimal("45.00"))
                .isActive(true)
                .build();

        Mockito.when(employeeService.registerNewEmployee(any(EmployeeRegisterRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/employees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("mechanic@erp.by"))
                .andExpect(jsonPath("$.role").value("MECHANIC"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Отказ в регистрации сотрудника, если роль пользователя не ADMIN")
    void shouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
        EmployeeRegisterRequest request = new EmployeeRegisterRequest();
        request.setEmail("valid.email@erp.by");
        request.setPassword("validPassword123");
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setRole("MECHANIC");

        mockMvc.perform(post("/api/v1/employees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Валидация: ошибка при некорректном формате email и роли")
    void shouldReturnBadRequestWhenRegistrationDataIsInvalid() throws Exception {
        EmployeeRegisterRequest request = new EmployeeRegisterRequest();
        request.setEmail("invalid-email"); // Некорректный email
        request.setPassword("123"); // Меньше 6 символов
        request.setFirstName(""); // Пустое поле
        request.setLastName("Иванов");
        request.setRole("INVALID_ROLE"); // Не проходит Pattern

        mockMvc.perform(post("/api/v1/employees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Успешное увольнение сотрудника")
    void shouldDismissEmployee() throws Exception {
        mockMvc.perform(patch("/api/v1/employees/{id}/dismiss", 1L))
                .andExpect(status().isOk());

        Mockito.verify(employeeService, Mockito.times(1)).dismissEmployee(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Успешное обновление email сотрудника")
    void shouldUpdateEmail() throws Exception {
        UpdateEmailRequestDto dto = new UpdateEmailRequestDto();
        dto.setNewEmail("new.email@erp.by");

        mockMvc.perform(patch("/api/v1/employees/{id}/email", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        Mockito.verify(employeeService, Mockito.times(1)).updateEmail(1L, "new.email@erp.by");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Получение списка сотрудников по роли (Slice пагинация)")
    void shouldGetEmployeesByRole() throws Exception {
        EmployeeResponseDto responseDto = EmployeeResponseDto.builder()
                .id(2L)
                .email("master@erp.by")
                .role("MASTER")
                .build();

        SliceImpl<EmployeeResponseDto> slice = new SliceImpl<>(List.of(responseDto), PageRequest.of(0, 20), false);

        Mockito.when(employeeService.getEmployeesByRole(eq("MASTER"), any()))
                .thenReturn(slice);

        mockMvc.perform(get("/api/v1/employees/role/{role}", "master"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2L))
                .andExpect(jsonPath("$.content[0].role").value("MASTER"));
    }
}
