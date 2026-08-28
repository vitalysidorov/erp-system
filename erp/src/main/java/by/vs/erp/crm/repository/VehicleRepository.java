package by.vs.erp.crm.repository;

import by.vs.erp.crm.entity.Vehicle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByVin(String vin);

    Slice<Vehicle> findByClientId(Long clientId, Pageable pageable);
}
