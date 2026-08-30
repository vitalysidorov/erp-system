package by.vs.erp.crm.service;

import by.vs.erp.common.exception.ConflictException;
import by.vs.erp.crm.dto.ClientDto;
import by.vs.erp.crm.dto.ClientReadDto;
import by.vs.erp.crm.dto.ClientRegisterRequest;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.mapper.ClientMapper;
import by.vs.erp.crm.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientService clientService;

    @Test
    @DisplayName("registerNewClient: Успешная регистрация нового клиента с хэшированием пароля")
    void registerNewClient_Success() {
        ClientRegisterRequest request = ClientRegisterRequest.builder()
                .phone("+375291112233")
                .password("plainPassword")
                .firstName("Иван")
                .lastName("Иванов")
                .build();

        when(clientRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");

        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client clientToSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(clientToSave, "id", 100L); // Эмуляция ID
            return clientToSave;
        });

        ClientReadDto result = clientService.registerNewClient(request);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("+375291112233", result.getPhone());
        assertEquals("Иван", result.getFirstName());
        assertEquals("Иванов", result.getLastName());

        // Проверяем, что в репозиторий ушел объект с захэшированным паролем и дефолтной ролью
        verify(passwordEncoder, times(1)).encode("plainPassword");
        verify(clientRepository, times(1)).save(argThat(client ->
                client.getPhone().equals("+375291112233") &&
                        client.getPassword().equals("hashedPassword") &&
                        client.getRole().equals("CLIENT")
        ));
    }

    @Test
    @DisplayName("registerNewClient: Выброс ConflictException, если телефон уже занят")
    void registerNewClient_ThrowsConflictException_WhenPhoneExists() {
        ClientRegisterRequest request = ClientRegisterRequest.builder()
                .phone("+375291112233")
                .build();

        when(clientRepository.existsByPhone(request.getPhone())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> clientService.registerNewClient(request));

        assertEquals("Клиент с таким номером телефона уже зарегистрирован", exception.getMessage());
        verify(clientRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }


    @Test
    @DisplayName("createClient: Успешное создание клиента через маппер")
    void createClient_Success() {
        ClientDto clientDto = new ClientDto("+375291112233", "pass", "Иванов", "Иван");
        Client clientEntity = new Client();
        Client savedEntity = new Client();
        ReflectionTestUtils.setField(savedEntity, "id", 200L);
        ClientReadDto expectedDto = new ClientReadDto(200L, "+375291112233", "Иван", "Иванов");

        when(clientMapper.toEntity(clientDto)).thenReturn(clientEntity);
        when(clientRepository.save(clientEntity)).thenReturn(savedEntity);
        when(clientMapper.toDto(savedEntity)).thenReturn(expectedDto);

        ClientReadDto result = clientService.createClient(clientDto);

        assertNotNull(result);
        assertEquals(200L, result.getId());
        verify(clientRepository, times(1)).save(clientEntity);
    }

    @Test
    @DisplayName("findByPhone: Возврат Optional с DTO, если клиент найден в БД")
    void findByPhone_ReturnsClient_WhenFound() {
        String phone = "+375291112233";
        Client client = new Client();
        ClientReadDto expectedDto = new ClientReadDto(1L, phone, "Иван", "Иванов");

        when(clientRepository.findByPhone(phone)).thenReturn(Optional.of(client));
        when(clientMapper.toDto(client)).thenReturn(expectedDto);

        Optional<ClientReadDto> result = clientService.findByPhone(phone);

        assertTrue(result.isPresent());
        assertEquals(phone, result.get().getPhone());
    }

    @Test
    @DisplayName("findByPhone: Возврат Optional.empty(), если клиент отсутствует в БД")
    void findByPhone_ReturnsEmpty_WhenNotFound() {
        String phone = "+375290000000";
        when(clientRepository.findByPhone(phone)).thenReturn(Optional.empty());

        Optional<ClientReadDto> result = clientService.findByPhone(phone);

        assertFalse(result.isPresent());
        verifyNoInteractions(clientMapper);
    }

    @Test
    @DisplayName("getAllClients: Успешный возврат Slice/Page страниц с маппингом данных")
    void getAllClients_ReturnsSlice() {
        Pageable pageable = PageRequest.of(0, 10);
        Client client = new Client();
        Page<Client> clientPage = new PageImpl<>(List.of(client));
        ClientReadDto mappedDto = new ClientReadDto(1L, "+375291112233", "Иван", "Иванов");

        when(clientRepository.findAll(pageable)).thenReturn(clientPage);
        when(clientMapper.toDto(client)).thenReturn(mappedDto);

        Slice<ClientReadDto> result = clientService.getAllClients(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("+375291112233", result.getContent().get(0).getPhone());
        verify(clientRepository, times(1)).findAll(pageable);
    }
}
