package by.vs.erp.order.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import by.vs.erp.order.dto.CreateWorkOrderRequestDto;
import by.vs.erp.order.dto.WorkOrderResponseDto;
import by.vs.erp.order.entity.WorkOrder;
import by.vs.erp.order.mapper.WorkOrderMapper;
import by.vs.erp.order.repository.WorkOrderRepository;
import by.vs.erp.order.service.ServiceCatalogService;
import by.vs.erp.order.service.WorkOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkOrderRestControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private WorkOrderService workOrderService;
    @MockBean private WorkOrderRepository workOrderRepository;
    @MockBean private EmployeeRepository employeeRepository;
    @MockBean private ServiceCatalogService serviceCatalogService;
    @MockBean private WorkOrderMapper workOrderMapper;

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /work-orders: Успешное открытие заказ-наряда ролью MANAGER")
    void shouldOpenWorkOrderWhenUserIsManager() throws Exception {
        CreateWorkOrderRequestDto requestDto = new CreateWorkOrderRequestDto(
                "+375291112233", "Иван", "Петров",
                "WBAFF71000B123456", "BMW", "530d", "1111 AX-7",
                1L, 185000, "1/4", "Мелкие царапины"
        );

        WorkOrderResponseDto responseDto = new WorkOrderResponseDto();
        responseDto.setId(100L);
        responseDto.setStatus("OPENED");

        Mockito.when(workOrderService.createWorkOrder(any(CreateWorkOrderRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.status").value("OPENED"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /work-orders: Ошибка валидации (400) при неверной длине VIN-номера")
    void shouldReturnBadRequestWhenVinIsInvalid() throws Exception {
        CreateWorkOrderRequestDto requestDto = new CreateWorkOrderRequestDto(
                "+375291112233", "Иван", "Петров",
                "BAD-VIN", "BMW", "530d", "1111 AX-7",
                1L, 185000, "1/4", "Мелкие царапины"
        );

        mockMvc.perform(post("/api/v1/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()); // Срабатывает @Size на VIN
    }

    @Test
    @DisplayName("PATCH /id/status: Успешное изменение статуса с извлечением @AuthenticationPrincipal")
    void shouldChangeStatusSuccessfully() throws Exception {
        Long orderId = 1L;
        String newStatus = "IN_PROGRESS";
        String managerEmail = "manager@erp.by";

        Employee manager = new Employee();
        manager.setEmail(managerEmail);

        Mockito.when(employeeRepository.findByEmail(managerEmail)).thenReturn(Optional.of(manager));

        org.springframework.security.core.GrantedAuthority authority =
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MANAGER");

        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(managerEmail); // Для @AuthenticationPrincipal String
        Mockito.when(authentication.getAuthorities()).thenAnswer(inv -> List.of(authority)); // Для @PreAuthorize hasRole
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(patch("/api/v1/work-orders/{id}/status", orderId)
                        .param("newStatus", newStatus))
                .andExpect(status().isOk());

        Mockito.verify(workOrderService, Mockito.times(1))
                .changeOrderStatus(eq(orderId), eq(newStatus), eq(manager));

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }


    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /active: Возврат пагинированного списка активных заказ-нарядов")
    void shouldGetActiveOrders() throws Exception {
        WorkOrder order = new WorkOrder();
        WorkOrderResponseDto responseDto = new WorkOrderResponseDto();
        responseDto.setStatus("IN_PROGRESS");

        PageImpl<WorkOrder> page = new PageImpl<>(List.of(order));

        Mockito.when(workOrderRepository.findByStatus(eq("IN_PROGRESS"), any(PageRequest.class)))
                .thenReturn(page);
        Mockito.when(workOrderMapper.toResponseDto(order)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/work-orders/active")
                        .param("page", "0")
                        .param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /vin: Получение списка Slice заказ-нарядов по VIN")
    void shouldGetOrdersByVin() throws Exception {
        String vin = "WBAFF71000B123456";
        WorkOrder order = new WorkOrder();

        WorkOrderResponseDto responseDto = new WorkOrderResponseDto();
        responseDto.setId(77L);
        responseDto.setVin(vin);

        SliceImpl<WorkOrder> slice = new SliceImpl<>(List.of(order));

        Mockito.when(workOrderRepository.findByVin(eq(vin), any(PageRequest.class)))
                .thenReturn(slice);
        Mockito.when(workOrderMapper.toResponseDto(order)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/work-orders/vin")
                        .param("vin", vin)
                        .param("page", "0")
                        .param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(77L))
                .andExpect(jsonPath("$.content[0].vin").value(vin));
    }
}
