package by.vs.erp.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "ord_car_classes")
@Getter
@Setter
public class CarClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String brand;

    @Column(name = "price_coefficient", nullable = false, precision = 3, scale = 2)
    private BigDecimal priceCoefficient = BigDecimal.valueOf(1.00);
}
