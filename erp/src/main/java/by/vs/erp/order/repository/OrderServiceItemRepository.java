package by.vs.erp.order.repository;

import by.vs.erp.order.entity.OrderServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderServiceItemRepository extends JpaRepository<OrderServiceItem, Long> {

    // поиск задач для конкретного механика (использует индекс idx_order_services_mechanic)
    @Query("SELECT s FROM OrderServiceItem s " +
            "JOIN FETCH s.workOrder w " +
            "JOIN FETCH s.service sc " +
            "WHERE s.mechanic.id = :mechanicId AND w.status = 'IN_PROGRESS'")
    List<OrderServiceItem> findActiveTasksByMechanic(@Param("mechanicId") Long mechanicId);
}
