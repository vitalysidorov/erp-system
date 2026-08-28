package by.vs.erp.crm.service;

import by.vs.erp.crm.dto.VehicleDto;
import by.vs.erp.crm.dto.VehicleReadDto;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.entity.Vehicle;
import by.vs.erp.crm.mapper.VehicleMapper;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.crm.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    @DisplayName("Должен привязать транспорт к клиенту")
    void addVehicleToClient_ClientExists_Success() {
        Long clientId = 1L;
        VehicleDto dto = new VehicleDto("12345678901234567", clientId, "Tesla", "Model S", "1111 AA-7");

        Client client = new Client();
        client.setId(clientId);
        client.setVehicles(new ArrayList<>());

        Vehicle vehicleEntity = new Vehicle();
        VehicleReadDto readDto = new VehicleReadDto(10L, "12345678901234567", "1", "Tesla", "Model S");

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(vehicleMapper.toEntity(dto)).thenReturn(vehicleEntity);
        when(clientRepository.save(client)).thenReturn(client);
        when(vehicleMapper.toDto(any())).thenReturn(readDto);

        VehicleReadDto result = vehicleService.addVehicleToClient(clientId, dto);

        assertNotNull(result);
        assertEquals("Tesla", result.getMake());
        verify(clientRepository, times(1)).save(client);
    }

    @Test
    @DisplayName("Попытка привязать транспорт к несуществующему клиенту")
    void addVehicleToClient_ClientNotFound_ThrowsIllegalArgumentException() {
        Long clientId = 99L;
        VehicleDto dto = new VehicleDto("12345678901234567", clientId, "Tesla", "Model S", "1111 AA-7");

        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                vehicleService.addVehicleToClient(clientId, dto)
        );

        assertEquals("Клиент с ID " + clientId + " не найден в базе CRM", exception.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен вернуть пустой Optional<> по отсутствующему в базе vin")
    void findByVin_NonExistentVin_ReturnsEmptyOptional() {
        String vin = "NOT_EXIST_VIN_123";
        when(vehicleRepository.findByVin(vin)).thenReturn(Optional.empty());

        Optional<VehicleReadDto> result = vehicleService.findByVin(vin);

        assertTrue(result.isEmpty());
        verify(vehicleMapper, never()).toDto(any(Vehicle.class));
    }

    @Test
    @DisplayName("Должен вывести весь транспорт пользователя")
    void getClientVehicles_ClientExists_ReturnsSlice() {
        Long clientId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Vehicle vehicle = new Vehicle();
        VehicleReadDto readDto = new VehicleReadDto(10L, "12345678901234567", "1", "Tesla", "Model S");

        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(vehicleRepository.findByClientId(eq(clientId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vehicle)));
        when(vehicleMapper.toDto(vehicle)).thenReturn(readDto);

        Slice<VehicleReadDto> result = vehicleService.getClientVehicles(clientId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Tesla", result.getContent().get(0).getMake());
    }

    @Test
    @DisplayName("Попытка вывести весь транспорт несуществующего пользователя")
    void getClientVehicles_ClientDoesNotExist_ThrowsIllegalArgumentException() {
        Long clientId = 99L;
        Pageable pageable = PageRequest.of(0, 10);
        when(clientRepository.existsById(clientId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                vehicleService.getClientVehicles(clientId, pageable)
        );

        assertEquals("Клиент с ID " + 99L + " не существует", exception.getMessage());
        verify(vehicleRepository, never()).findByClientId(anyLong(), any());
    }
}
