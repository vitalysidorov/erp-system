package by.vs.erp.order.entity;

import by.vs.erp.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ord_order_status_log")
@Getter
@Setter
public class WorkOrderStatusLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private Employee changedBy;
}

