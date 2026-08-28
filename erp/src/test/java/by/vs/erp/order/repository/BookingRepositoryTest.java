package by.vs.erp.order.repository;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.entity.Vehicle;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.crm.repository.VehicleRepository;
import by.vs.erp.order.entity.Booking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BookingRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("Booking: Должен сохранять бронь автомобиля с дефолтным источником MANUAL и статусом PENDING")
    void shouldSaveBookingWithDefaultValues() {
        Client client = new Client();
        client.setFirstName("Игорь");
        client.setLastName("Николаев");
        client.setPhone("+375294443322");
        clientRepository.save(client);

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(client);
        vehicle.setVin("VINBOOKING1234567");
        vehicle.setMake("Toyota");
        vehicle.setModel("Camry");
        vehicleRepository.saveAndFlush(vehicle);

        Booking booking = new Booking();
        booking.setCreatedBy("operator@erp.by");
        booking.setVehicle(vehicle);
        booking.setStartTime(LocalDateTime.now().plusDays(2));
        booking.setEndTime(LocalDateTime.now().plusDays(2).plusHours(2));

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        assertNotNull(savedBooking.getId());
        assertEquals("MANUAL", savedBooking.getSource(), "Должно сработать дефолтное значение поля source");
        assertEquals("PENDING", savedBooking.getStatus(), "Должно сработать дефолтное значение статуса");
    }
}
