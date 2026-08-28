package by.vs.erp.order.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.order.dto.MechanicTaskDto;
import by.vs.erp.order.service.MechanicTaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MechanicTaskRestControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MechanicTaskService mechanicTaskService;

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("GET /tasks/{id}: Успешное получение списка активных задач мастером")
    void shouldGetActiveTasksWhenUserIsMaster() throws Exception {

        Long mechanicId = 1L;
        MechanicTaskDto task = new MechanicTaskDto(50L, 100L, "Audi A4", "Замена сцепления", new BigDecimal("2.50"));

        Mockito.when(mechanicTaskService.getActiveTasksForMechanic(eq(mechanicId)))
                .thenReturn(List.of(task));

        mockMvc.perform(get("/api/v1/mechanic/tasks/{mechanicId}", mechanicId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(50L))
                .andExpect(jsonPath("$[0].carInfo").value("Audi A4"))
                .andExpect(jsonPath("$[0].serviceName").value("Замена сцепления"));
    }
}
