package by.vs.erp.crm.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.crm.dto.ClientReadDto;
import by.vs.erp.crm.dto.ClientRegisterRequest;
import by.vs.erp.crm.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
class ClientControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    @Test
    @DisplayName("POST /register: Успешная регистрация клиента без авторизации (PermitAll)")
    void register_Success() throws Exception {
        // Given: Собираем валидный запрос через билдер
        ClientRegisterRequest request = ClientRegisterRequest.builder()
                .phone("+375291112233")
                .password("securePassword123") // Длина >= 6 символов, валидация пройдет
                .lastName("Иванов")
                .firstName("Иван")
                .build();

        ClientReadDto expectedResponse = new ClientReadDto(
                10L, "+375291112233", "Иванов", "Иван"
        );

        Mockito.when(clientService.registerNewClient(any(ClientRegisterRequest.class)))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/clients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.phone").value("+375291112233"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.firstName").value("Иван"));

        Mockito.verify(clientService, Mockito.times(1)).registerNewClient(request);
    }

    @Test
    @DisplayName("POST /register: Ошибка 400 (Bad Request), если переданы пустые поля")
    void register_ValidationError_WhenFieldsAreBlank() throws Exception {
        // Given: Собираем невалидный запрос (пустые строки)
        ClientRegisterRequest invalidRequest = ClientRegisterRequest.builder()
                .phone("")
                .password("")
                .lastName("")
                .firstName("")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/clients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // Ожидаем 400 из-за @NotBlank

        Mockito.verifyNoInteractions(clientService);
    }

    @Test
    @DisplayName("POST /register: Ошибка 400 (Bad Request), если пароль слишком короткий")
    void register_ValidationError_WhenPasswordTooShort() throws Exception {
        // Given: Пароль меньше 6 символов (нарушение условия @Size(min = 6))
        ClientRegisterRequest invalidRequest = ClientRegisterRequest.builder()
                .phone("+375291112233")
                .password("123")
                .lastName("Иванов")
                .firstName("Иван")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/clients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(clientService);
    }
}
