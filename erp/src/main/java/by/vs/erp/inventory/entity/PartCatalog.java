package by.vs.erp.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inv_parts_catalog")
@Getter
@Setter
public class PartCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oem_number", nullable = false, unique = true, length = 50)
    private String oemNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String brand;
}
