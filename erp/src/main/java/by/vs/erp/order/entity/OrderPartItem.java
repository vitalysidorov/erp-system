package by.vs.erp.order.entity;

import by.vs.erp.inventory.entity.PartCatalog;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "ord_order_parts")
@Getter
@Setter
public class OrderPartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private PartCatalog part;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "final_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice; // Историческая фиксация цены продажи
}

