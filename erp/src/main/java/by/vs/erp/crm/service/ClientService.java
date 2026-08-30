package by.vs.erp.crm.service;

import by.vs.erp.common.exception.ConflictException;
import by.vs.erp.crm.dto.ClientDto;
import by.vs.erp.crm.dto.ClientReadDto;
import by.vs.erp.crm.dto.ClientRegisterRequest;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.mapper.ClientMapper;
import by.vs.erp.crm.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ClientReadDto registerNewClient(ClientRegisterRequest request) {
        if (clientRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Клиент с таким номером телефона уже зарегистрирован");
        }

        Client client = new Client();
        client.setPhone(request.getPhone());
        client.setLastName(request.getLastName());
        client.setFirstName(request.getFirstName());
        client.setRole("CLIENT");
        client.setPassword(passwordEncoder.encode(request.getPassword()));

        Client saved = clientRepository.save(client);
        return new ClientReadDto(saved.getId(), saved.getPhone(), saved.getLastName(), saved.getFirstName());
    }

    @Transactional
    @CacheEvict(value = "crm::clients", key = "#clientDto.phone")
    public ClientReadDto createClient(ClientDto clientDto) {
        return clientMapper.toDto(clientRepository.save(clientMapper.toEntity(clientDto)));
    }

    @Cacheable(value = "crm::clients", key = "#phone")
    @Transactional(readOnly = true)
    public Optional<ClientReadDto> findByPhone(String phone) {
        return clientRepository.findByPhone(phone)
                .map(clientMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Slice<ClientReadDto> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable).map(clientMapper::toDto);
    }
}
