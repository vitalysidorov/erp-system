package by.vs.erp.inventory.entity;

import by.vs.erp.inventory.listener.StockCacheListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "inv_stocks")
@Getter
@Setter
@EntityListeners(StockCacheListener.class)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false, unique = true)
    private PartCatalog part;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "retail_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;
}
