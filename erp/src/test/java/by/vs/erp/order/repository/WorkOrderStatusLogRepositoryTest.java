package by.vs.erp.order.repository;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.entity.Vehicle;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.crm.repository.VehicleRepository;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import by.vs.erp.order.entity.WorkOrder;
import by.vs.erp.order.entity.WorkOrderStatusLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderStatusLogRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private WorkOrderStatusLogRepository statusLogRepository;

    @Test
    @DisplayName("StatusLog: Успешное сохранение записи лога при переводе заказа в новый статус")
    void shouldSaveStatusLogEntry() {
        Employee manager = new Employee();
        manager.setEmail("manager-log@erp.by");
        manager.setPassword("hash");
        manager.setFirstName("Анна");
        manager.setLastName("Админова");
        manager.setRole("MANAGER");
        manager.setSalaryRatePercent(BigDecimal.valueOf(100));
        employeeRepository.save(manager);

        Client client = new Client();
        client.setFirstName("Иван");
        client.setLastName("Логинов");
        client.setPhone("+375291234567");
        clientRepository.save(client);

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(client);
        vehicle.setVin("VINLOGTEST1234567");
        vehicle.setMake("Ford");
        vehicle.setModel("Focus");
        vehicleRepository.save(vehicle);

        WorkOrder order = new WorkOrder();
        order.setVehicle(vehicle);
        order.setMaster(manager);
        order.setStatus("IN_PROGRESS");
        order.setMileageIn(100000);
        WorkOrder savedOrder = workOrderRepository.saveAndFlush(order);

        WorkOrderStatusLog logEntry = new WorkOrderStatusLog();
        logEntry.setWorkOrder(savedOrder);
        logEntry.setFromStatus("OPENED");
        logEntry.setToStatus("IN_PROGRESS");
        logEntry.setChangedBy(manager);

        WorkOrderStatusLog savedLog = statusLogRepository.saveAndFlush(logEntry);

        assertNotNull(savedLog.getId(), "ID записи лога должен быть сгенерирован базой данных");
        assertNotNull(savedLog.getChangedAt(), "Время изменения должно проставиться автоматически по умолчанию");
        assertEquals("OPENED", savedLog.getFromStatus());
        assertEquals("IN_PROGRESS", savedLog.getToStatus());
        assertEquals(manager.getId(), savedLog.getChangedBy().getId());
    }
}
