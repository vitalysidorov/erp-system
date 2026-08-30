package by.vs.erp.employee.service;

import by.vs.erp.common.security.TokenHasher;
import by.vs.erp.employee.dto.LoginRequest;
import by.vs.erp.employee.dto.JwtResponse;
import by.vs.erp.employee.dto.RefreshRequest;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.common.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;

    @Transactional
    public JwtResponse login(LoginRequest request) {
        if (request.getEmail() != null && request.getEmail().contains("@")) {
            Employee employee = employeeRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Сотрудник не найден: " + request.getEmail()));

            if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
                throw new BadCredentialsException("Неверный пароль");
            }
            if (employee.getIsActive() != null && !employee.getIsActive()) {
                throw new BadCredentialsException("Аккаунт сотрудника деактивирован");
            }

            String accessToken = jwtProvider.generateAccessToken(employee.getId().toString(), employee.getEmail(), employee.getRole());
            String refreshToken = jwtProvider.generateRefreshToken(employee.getEmail());

            employee.setRefreshToken(tokenHasher.hash(refreshToken));
            employeeRepository.save(employee);
            return new JwtResponse(accessToken, refreshToken);
        } else {
            Client client = clientRepository.findByPhone(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Клиент с таким телефоном не найден"));

            if (!passwordEncoder.matches(request.getPassword(), client.getPassword())) {
                throw new BadCredentialsException("Неверный пароль");
            }

            String accessToken = jwtProvider.generateAccessToken(client.getId().toString(), client.getPhone(), client.getRole());
            String refreshToken = jwtProvider.generateRefreshToken(client.getPhone());

            client.setRefreshToken(tokenHasher.hash(refreshToken));
            clientRepository.save(client);
            return new JwtResponse(accessToken, refreshToken);
        }
    }

    @Transactional
    public JwtResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Невалидный или просроченный Refresh токен");
        }

        String loginOrEmail = jwtProvider.getRefreshClaims(refreshToken).getSubject();
        String incomingHash = tokenHasher.hash(refreshToken);

        var employeeOpt = employeeRepository.findByEmail(loginOrEmail);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            if (!employee.getIsActive()) throw new BadCredentialsException("Аккаунт деактивирован");
            if (employee.getRefreshToken() == null || !employee.getRefreshToken().equals(incomingHash)) {
                throw new BadCredentialsException("Refresh токен отозван");
            }

            String newAccess = jwtProvider.generateAccessToken(employee.getId().toString(), employee.getEmail(), employee.getRole());
            String newRefresh = jwtProvider.generateRefreshToken(employee.getEmail());
            employee.setRefreshToken(tokenHasher.hash(newRefresh));
            employeeRepository.save(employee);
            return new JwtResponse(newAccess, newRefresh);
        }

        Client client = clientRepository.findByPhone(loginOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        if (client.getRefreshToken() == null || !client.getRefreshToken().equals(incomingHash)) {
            throw new BadCredentialsException("Refresh токен отозван");
        }

        String newAccess = jwtProvider.generateAccessToken(client.getId().toString(), client.getPhone(), client.getRole());
        String newRefresh = jwtProvider.generateRefreshToken(client.getPhone());
        client.setRefreshToken(tokenHasher.hash(newRefresh));
        clientRepository.save(client);
        return new JwtResponse(newAccess, newRefresh);
    }
}