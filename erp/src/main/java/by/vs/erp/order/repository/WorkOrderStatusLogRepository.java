package by.vs.erp.order.repository;

import by.vs.erp.order.entity.WorkOrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderStatusLogRepository extends JpaRepository<WorkOrderStatusLog, Long> {
}
