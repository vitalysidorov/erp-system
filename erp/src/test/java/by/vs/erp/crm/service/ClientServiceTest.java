package by.vs.erp.crm.service;

import by.vs.erp.crm.dto.ClientDto;
import by.vs.erp.crm.dto.ClientReadDto;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.mapper.ClientMapper;
import by.vs.erp.crm.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
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

    @InjectMocks
    private ClientService clientService;

    private Client client;
    private ClientDto clientDto;
    private ClientReadDto clientReadDto;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
        client.setPhone("+375291112233");

        clientDto = new ClientDto("+375291112233", "Иванов", "Иван");
        clientReadDto = new ClientReadDto(1L, "+375291112233", "Иванов", "Иван");
    }

    @Test
    @DisplayName("Должен создать клиента")
    void createClient_Success() {
        when(clientMapper.toEntity(clientDto)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toDto(client)).thenReturn(clientReadDto);

        ClientReadDto result = clientService.createClient(clientDto);

        assertNotNull(result);
        assertEquals(clientReadDto.getId(), result.getId());

        verify(clientRepository, times(1)).save(client);
    }

    @Test
    @DisplayName("Должен найти по номеру телефона")
    void findByPhone_ExistingPhone_ReturnsClientDto() {
        String phone = "+375291112233";
        when(clientRepository.findByPhone(phone)).thenReturn(Optional.of(client));
        when(clientMapper.toDto(client)).thenReturn(clientReadDto);

        Optional<ClientReadDto> result = clientService.findByPhone(phone);

        assertTrue(result.isPresent());
        assertEquals(phone, result.get().getPhone());
        verify(clientRepository, times(1)).findByPhone(phone);
    }

    @Test
    @DisplayName("Должен вернуть пустой Optional<> по отсутствующему в базе номеру телефона")
    void findByPhone_NonExistentPhone_ReturnsEmptyOptional() {
        String phone = "+375290000000";
        when(clientRepository.findByPhone(phone)).thenReturn(Optional.empty());

        Optional<ClientReadDto> result = clientService.findByPhone(phone);

        assertTrue(result.isEmpty());
        verify(clientMapper, never()).toDto(any(Client.class));
    }

    @Test
    @DisplayName("Должен вывести всех пользователей")
    void getAllClients_ReturnsSlice() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Client> clientPage = new PageImpl<>(List.of(client));

        when(clientRepository.findAll(any(Pageable.class))).thenReturn(clientPage);
        when(clientMapper.toDto(client)).thenReturn(clientReadDto);

        Slice<ClientReadDto> result = clientService.getAllClients(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(clientReadDto.getLastName(), result.getContent().get(0).getLastName());
        verify(clientRepository, times(1)).findAll(pageable);
    }
}
