package by.vs.erp.order.repository;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.entity.Vehicle;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.crm.repository.VehicleRepository;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import by.vs.erp.inventory.entity.PartCatalog;
import by.vs.erp.inventory.repository.PartCatalogRepository;
import by.vs.erp.order.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderCascadeRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private PartCatalogRepository partCatalogRepository;

    @Test
    @DisplayName("Cascade: Сохранение WorkOrder должно автоматически записывать связанные позиции услуг и запчастей")
    void shouldSaveWorkOrderWithItemsCascading() {
        Client client = new Client();
        client.setFirstName("Дмитрий");
        client.setLastName("Ковалев");
        client.setPhone("+375299998877");
        clientRepository.save(client);

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(client);
        vehicle.setVin("WBA00000000TEST12");
        vehicle.setMake("BMW");
        vehicle.setModel("X5");
        vehicleRepository.save(vehicle);

        Employee master = new Employee();
        master.setEmail("manager@erp.by");
        master.setPassword("hash");
        master.setFirstName("Петр");
        master.setLastName("Приемщиков");
        master.setRole("MANAGER");
        master.setSalaryRatePercent(BigDecimal.valueOf(100));
        employeeRepository.save(master);

        Employee mechanic = new Employee();
        mechanic.setEmail("master@erp.by");
        mechanic.setPassword("hash");
        mechanic.setFirstName("Сергей");
        mechanic.setLastName("Ремонтеров");
        mechanic.setRole("MASTER");
        mechanic.setSalaryRatePercent(BigDecimal.valueOf(100));
        employeeRepository.save(mechanic);

        ServiceCatalog service = new ServiceCatalog();
        service.setName("Замена масла в ДВС");
        service.setNormHours(new BigDecimal("1.00"));
        service.setHourRatePrice(new BigDecimal("40.00"));
        serviceCatalogRepository.save(service);

        PartCatalog part = new PartCatalog();
        part.setOemNumber("5W40-SHELL");
        part.setName("Масло Shell Helix 5W-40");
        part.setBrand("Shell");
        partCatalogRepository.save(part);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setVehicle(vehicle);
        workOrder.setMaster(master);
        workOrder.setStatus("OPENED");
        workOrder.setMileageIn(85000);
        workOrder.setFuelLevel("1/2");

        OrderServiceItem serviceItem = new OrderServiceItem();
        serviceItem.setWorkOrder(workOrder);
        serviceItem.setService(service);
        serviceItem.setMechanic(mechanic);
        serviceItem.setQuantity(1);
        serviceItem.setFinalPrice(new BigDecimal("40.00"));
        workOrder.getServices().add(serviceItem);

        OrderPartItem partItem = new OrderPartItem();
        partItem.setWorkOrder(workOrder);
        partItem.setPart(part);
        partItem.setQuantity(4);
        partItem.setFinalPrice(new BigDecimal("15.00"));
        workOrder.getParts().add(partItem);

        WorkOrder savedOrder = workOrderRepository.saveAndFlush(workOrder);

        assertNotNull(savedOrder.getId(), "Идентификатор заказ-наряда должен быть сгенерирован БД");
        assertNotNull(savedOrder.getCreatedAt(), "Дата создания должна генерироваться автоматически через @PrePersist");

        workOrderRepository.flush();

        Optional<WorkOrder> fetchedOrderOpt = workOrderRepository.findById(savedOrder.getId());
        assertTrue(fetchedOrderOpt.isPresent(), "Заказ-наряд должен успешно находиться по ID");
        WorkOrder fetchedOrder = fetchedOrderOpt.get();

        assertEquals(1, fetchedOrder.getServices().size(), "Запись услуги должна сохраниться в БД каскадно");
        assertEquals("Замена масла в ДВС", fetchedOrder.getServices().get(0).getService().getName());
        assertEquals(0, new BigDecimal("40.00").compareTo(fetchedOrder.getServices().get(0).getFinalPrice()));
        assertEquals("Сергей", fetchedOrder.getServices().get(0).getMechanic().getFirstName());

        assertEquals(1, fetchedOrder.getParts().size(), "Запись использованной запчасти должна сохраниться каскадно");
        assertEquals(4, fetchedOrder.getParts().get(0).getQuantity());
        assertEquals("5W40-SHELL", fetchedOrder.getParts().get(0).getPart().getOemNumber());
        assertEquals(0, new BigDecimal("15.00").compareTo(fetchedOrder.getParts().get(0).getFinalPrice()));
    }
}
