package by.vs.erp.order.entity;

import by.vs.erp.order.listener.ServiceCatalogListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "ord_services_catalog")
@Getter
@Setter
@EntityListeners(ServiceCatalogListener.class)
public class ServiceCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "norm_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal normHours;

    @Column(name = "hour_rate_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal hourRatePrice;
}
