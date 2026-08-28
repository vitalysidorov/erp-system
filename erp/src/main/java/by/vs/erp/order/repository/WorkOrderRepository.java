package by.vs.erp.order.repository;

import by.vs.erp.order.entity.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    @Query("SELECT DISTINCT w FROM WorkOrder w " +
            "LEFT JOIN FETCH w.services s " +
            "LEFT JOIN FETCH w.parts p " +
            "WHERE w.id = :id")
    Optional<WorkOrder> findByIdWithDetails(@Param("id") Long id);

    Page<WorkOrder> findByStatus(String status, Pageable pageable);

    @Query("SELECT COUNT(*) FROM WorkOrder w WHERE w.status = :status")
    Long countByStatus(String status);

    @Query("SELECT DISTINCT w FROM WorkOrder w " +
            "LEFT JOIN FETCH w.services s " +
            "LEFT JOIN FETCH w.parts p " +
            "LEFT JOIN FETCH w.vehicle v "+
            "WHERE v.vin = :vin")
    Slice<WorkOrder> findByVin(String vin, Pageable pageable);
}

