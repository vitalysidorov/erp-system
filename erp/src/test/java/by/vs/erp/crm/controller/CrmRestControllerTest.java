package by.vs.erp.crm.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.crm.dto.ClientDto;
import by.vs.erp.crm.dto.VehicleDto;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.crm.repository.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CrmRestControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @AfterEach
    void tearDown() {
        vehicleRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Сквозной интеграционный сценарий: создание клиента -> поиск -> добавление авто -> поиск авто -> получение списка")
    void shouldCreateClientAndThenAddVehicleAndSearchIt() throws Exception {
        ClientDto clientDto = new ClientDto("+375291112233", "securePassword123", "Иванов", "Иван");

        String clientResponse = mockMvc.perform(post("/api/v1/crm/clients/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.phone").value("+375291112233"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andReturn().getResponse().getContentAsString();

        Long clientId = objectMapper.readTree(clientResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/crm/clients/search")
                        .param("phone", "+375291112233"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId))
                .andExpect(jsonPath("$.phone").value("+375291112233"));

        String validVin = "1234567890ABCDEF1";
        VehicleDto vehicleDto = new VehicleDto(validVin, clientId, "Toyota", "Camry", "Без г-н");

        mockMvc.perform(post("/api/v1/crm/clients/{clientId}/vehicles", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicleDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.vin").value(validVin))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Camry"));

        mockMvc.perform(get("/api/v1/crm/vehicles/search")
                        .param("vin", validVin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin").value(validVin))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Camry"));

        mockMvc.perform(get("/api/v1/crm/clients/{clientId}/vehicles", clientId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].vin").value(validVin))
                .andExpect(jsonPath("$.content[0].model").value("Camry"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Интеграционный поиск: возврат 404, если запрашиваемый телефон отсутствует в БД")
    void shouldReturnNotFoundWhenClientDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/crm/clients/search")
                        .param("phone", "+375290000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Безопасность: возврат 403 (Forbidden) для неаутентифицированного запроса")
    void shouldReturnUnauthorizedWhenUserNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/crm/clients/search")
                        .param("phone", "+375291112233"))
                .andExpect(status().isUnauthorized());
    }
}
