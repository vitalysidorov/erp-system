package by.vs.erp.order.entity;

import by.vs.erp.crm.entity.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ord_bookings")
@Getter
@Setter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "MANUAL"; // MANUAL (через ERP), BFF_WEB (через сайт)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, CONFIRMED, CANCELLED
}

