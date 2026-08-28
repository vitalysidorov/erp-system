package by.vs.erp.order.repository;

import by.vs.erp.order.entity.CarClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CarClassRepository extends JpaRepository<CarClass, Long> {

    Optional<CarClass> findByBrand(String brand);
}
