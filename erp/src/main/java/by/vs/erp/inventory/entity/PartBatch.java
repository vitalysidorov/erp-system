package by.vs.erp.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inv_part_batches")
@Getter
@Setter
public class PartBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private PartCatalog part;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();
}

