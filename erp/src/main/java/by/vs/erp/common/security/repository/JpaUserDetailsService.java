package by.vs.erp.common.security.repository;

import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        var employeeOpt = employeeRepository.findByEmail(login);
        if (employeeOpt.isPresent()) {
            var emp = employeeOpt.get();
            return User.withUsername(emp.getEmail())
                    .password(emp.getPassword())
                    .authorities("ROLE_" + emp.getRole())
                    .disabled(emp.getIsActive() != null && !emp.getIsActive())
                    .build();
        }

        var clientOpt = clientRepository.findByPhone(login);
        if (clientOpt.isPresent()) {
            var cl = clientOpt.get();
            return User.withUsername(cl.getPhone())
                    .password(cl.getPassword())
                    .authorities("ROLE_" + cl.getRole())
                    .disabled(false)
                    .build();
        }

        throw new UsernameNotFoundException("Пользователь не найден: " + login);
    }
}
