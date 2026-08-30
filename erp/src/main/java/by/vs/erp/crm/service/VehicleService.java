package by.vs.erp.crm.service;

import by.vs.erp.common.exception.NotFoundException;
import by.vs.erp.crm.dto.VehicleDto;
import by.vs.erp.crm.dto.VehicleReadDto;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.mapper.VehicleMapper;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.crm.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional
    @CacheEvict(value = "crm::vehicles", key = "#vehicleDto.vin")
    public VehicleReadDto addVehicleToClient(Long clientId, VehicleDto vehicleDto) {

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Клиент с ID " + clientId + " не найден в базе CRM"));
        client.getVehicles().add(vehicleMapper.toEntity(vehicleDto));
        Client savedClient = clientRepository.save(client);

        return vehicleMapper.toDto(savedClient.getVehicles().getLast());
    }

    @Cacheable(value = "crm::vehicles", key = "#vin")
    @Transactional(readOnly = true)
    public Optional<VehicleReadDto> findByVin(String vin) {
        return vehicleRepository.findByVin(vin)
                .map(vehicleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Slice<VehicleReadDto> getClientVehicles(Long clientId, Pageable pageable) {
        if (!clientRepository.existsById(clientId))
            throw new NotFoundException("Клиент с ID " + clientId + " не существует");

        return vehicleRepository.findByClientId(clientId, pageable).map(vehicleMapper::toDto);
    }
}
