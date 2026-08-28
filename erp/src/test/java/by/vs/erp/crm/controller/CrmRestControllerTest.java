package by.vs.erp.crm.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.crm.dto.ClientDto;
import by.vs.erp.crm.dto.VehicleDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CrmRestControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldCreateClientAndThenAddVehicleAndSearchIt() throws Exception {
        ClientDto clientDto = new ClientDto("+375291112233", "Иванов", "Иван");

        String clientResponse = mockMvc.perform(post("/api/v1/crm/clients/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.phone").value("+375291112233"))
                .andReturn().getResponse().getContentAsString();

        Long clientId = objectMapper.readTree(clientResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/crm/clients/search")
                        .param("phone", "+375291112233"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId));

        String validVin = "1234567890ABCDEFG";
        VehicleDto vehicleDto = new VehicleDto(validVin, clientId, "Toyota", "Camry", "Без г-н");

        mockMvc.perform(post("/api/v1/crm/clients/{clientId}/vehicles", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicleDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.vin").value(validVin));

        mockMvc.perform(get("/api/v1/crm/vehicles/search")
                        .param("vin", validVin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin").value(validVin))
                .andExpect(jsonPath("$.model").value("Camry"));

        mockMvc.perform(get("/api/v1/crm/clients/{clientId}/vehicles", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].vin").value(validVin));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldReturnNotFoundWhenClientDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/crm/clients/search")
                        .param("phone", "+375290000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/crm/clients/search")
                        .param("phone", "+375291112233"))
                .andExpect(status().isForbidden());
    }
}
