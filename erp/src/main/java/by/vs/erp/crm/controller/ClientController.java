package by.vs.erp.crm.controller;

import by.vs.erp.crm.dto.ClientReadDto;
import by.vs.erp.crm.dto.ClientRegisterRequest;
import by.vs.erp.crm.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping("/register")
    public ResponseEntity<ClientReadDto> register(@Valid @RequestBody ClientRegisterRequest request) {
        ClientReadDto response = clientService.registerNewClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
