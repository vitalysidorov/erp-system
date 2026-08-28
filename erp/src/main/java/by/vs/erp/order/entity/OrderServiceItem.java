package by.vs.erp.order.entity;

import by.vs.erp.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "ord_order_services")
@Getter
@Setter
public class OrderServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceCatalog service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private Employee mechanic; // Исполнитель работы

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "final_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice; // Историческая фиксация цены
}
