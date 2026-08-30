package by.vs.erp.crm.repository;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.entity.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VehicleRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("Должен сохранить автомобиль для существующего клиента")
    void shouldSaveVehicleForClient() {
        Client client = new Client();
        client.setFirstName("Сергей");
        client.setLastName("Сергеев");
        client.setPhone("+375299990011");
        client.setPassword("123hadui9");
        client.setRole("CLIENT");
        Client savedClient = clientRepository.save(client);

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(savedClient);
        vehicle.setVin("1234567890ABCDEF1");
        vehicle.setMake("Audi");
        vehicle.setModel("A6");
        vehicle.setPlateNumber("1111 AX-7");

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        assertNotNull(savedVehicle.getId());
        assertEquals(savedClient.getId(), savedVehicle.getClient().getId());
    }

    @Test
    @DisplayName("Должен найти автомобиль по VIN")
    void shouldFindVehicleByVin() {
        Client client = new Client();
        client.setFirstName("Ольга");
        client.setLastName("Ольгина");
        client.setPhone("+375299990022");
        client.setPassword("123hadui9");
        client.setRole("CLIENT");
        Client savedClient = clientRepository.save(client);

        String targetVin = "1234567890ABCDEF2";
        Vehicle vehicle = new Vehicle();
        vehicle.setClient(savedClient);
        vehicle.setVin(targetVin);
        vehicle.setMake("BMW");
        vehicle.setModel("X5");
        vehicleRepository.save(vehicle);

        Optional<Vehicle> foundVehicle = vehicleRepository.findByVin(targetVin);

        assertTrue(foundVehicle.isPresent());
        assertEquals("X5", foundVehicle.get().getModel());
    }

    @Test
    @DisplayName("Должен удалить автомобили каскадно при удалении клиента (CascadeType.ALL)")
    void shouldCascadeDeleteVehiclesWhenClientIsDeleted() {
        Client client = new Client();
        client.setFirstName("Дмитрий");
        client.setLastName("Дмитриев");
        client.setPhone("+375299990033");
        client.setPassword("123hadui9");
        client.setRole("CLIENT");

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(client);
        vehicle.setVin("1234567890ABCDEF3");
        vehicle.setMake("Opel");
        vehicle.setModel("Astra");

        client.getVehicles().add(vehicle);

        Client savedClient = clientRepository.saveAndFlush(client);
        Long vehicleId = savedClient.getVehicles().get(0).getId();

        assertTrue(vehicleRepository.findById(vehicleId).isPresent());

        clientRepository.delete(savedClient);
        clientRepository.flush();

        Optional<Vehicle> deletedVehicle = vehicleRepository.findById(vehicleId);
        assertTrue(deletedVehicle.isEmpty(), "Автомобиль должен быть удален вместе с клиентом");
    }
}
