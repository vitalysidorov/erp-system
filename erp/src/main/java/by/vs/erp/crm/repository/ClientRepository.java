package by.vs.erp.crm.repository;

import by.vs.erp.crm.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByPhone(String clientPhone);

    boolean existsByPhone(String phone);
}