package by.vs.erp.order.entity;

import by.vs.erp.crm.entity.Vehicle;
import by.vs.erp.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ord_work_orders")
@Getter
@Setter
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private Employee master; // Мастер-приемщик

    @Column(nullable = false, length = 20)
    private String status = "OPENED"; // OPENED, IN_PROGRESS, COMPLETED, CLOSED

    @Column(name = "mileage_in", nullable = false)
    private Integer mileageIn;

    @Column(name = "fuel_level", length = 20)
    private String fuelLevel;

    @Column(name = "damages_notes", columnDefinition = "TEXT")
    private String damagesNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Каскадное сохранение: добавляя позиции в списки, они сохранятся вместе с заказ-нарядом
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderServiceItem> services = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPartItem> parts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

