package by.vs.erp.crm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "crm_vehicles")
@Getter
@Setter
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @Column(nullable = false, length = 50)
    private String make;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "plate_number", length = 20)
    private String plateNumber;
}
