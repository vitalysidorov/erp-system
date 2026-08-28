package by.vs.erp.employee.repository;

import by.vs.erp.employee.entity.Employee;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT e.isActive FROM Employee e WHERE e.email = :email")
    Optional<Boolean> findIsActiveByEmail(@Param("email") String email);

    Slice<Employee> findByRole(String role, Pageable pageable);

    Slice<Employee> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(String firstName,
                                                                                       String lastName,
                                                                                       Pageable pageable);
}
