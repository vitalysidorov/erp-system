package by.vs.erp.crm.controller;

import by.vs.erp.crm.dto.ClientDto;
import by.vs.erp.crm.dto.ClientReadDto;
import by.vs.erp.crm.dto.VehicleDto;
import by.vs.erp.crm.dto.VehicleReadDto;
import by.vs.erp.crm.service.ClientService;
import by.vs.erp.crm.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crm")
@RequiredArgsConstructor
@Slf4j
public class CrmRestController {

    private final ClientService clientService;
    private final VehicleService vehicleService;

    @PostMapping("/clients/create")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<ClientReadDto> createClient(@Valid @RequestBody ClientDto clientDto) {
        log.info("API: Регистрация нового клиента с телефоном: {}", clientDto.getPhone());
        ClientReadDto savedClient = clientService.createClient(clientDto);
        return new ResponseEntity<>(savedClient, HttpStatus.CREATED);
    }

    @GetMapping("/clients/search")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<ClientReadDto> findClientByPhone(@RequestParam String phone) {
        log.info("API: Поиск клиента по номеру телефона: {}", phone);
        return clientService.findByPhone(phone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/clients/{clientId}/vehicles")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<VehicleReadDto> addVehicleToClient(
            @PathVariable Long clientId,
            @Valid @RequestBody VehicleDto vehicleDto) {
        log.info("API: Привязка автомобиля с VIN: {} к клиенту ID: {}", vehicleDto.getVin(), clientId);
        VehicleReadDto savedVehicle = vehicleService.addVehicleToClient(clientId, vehicleDto);
        return new ResponseEntity<>(savedVehicle, HttpStatus.CREATED);
    }

    @GetMapping("/vehicles/search")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<VehicleReadDto> findVehicleByVin(@RequestParam String vin) {
        log.info("API: Поиск автомобиля по VIN коду: {}", vin);
        return vehicleService.findByVin(vin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/clients/{clientId}/vehicles")
    @PreAuthorize(value = "hasRole('MANAGER')")
    public ResponseEntity<Slice<VehicleReadDto>> getClientVehicles(@PathVariable Long clientId,
                                                                   @PageableDefault(size = 10) Pageable pageable) {
        log.info("API: Запрос списка автомобилей для клиента ID: {}", clientId);
        Slice<VehicleReadDto> vehicles = vehicleService.getClientVehicles(clientId, pageable);
        return ResponseEntity.ok(vehicles);
    }

    @PostMapping("/clients")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Slice<ClientReadDto>> getAllClients(@PageableDefault(size = 10) Pageable pageable) {
        log.info("API: Запрос полного списка клиентов");
        Slice<ClientReadDto> clients = clientService.getAllClients(pageable);
        return ResponseEntity.ok(clients);
    }
}

