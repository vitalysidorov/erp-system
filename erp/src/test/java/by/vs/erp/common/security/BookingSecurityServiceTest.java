package by.vs.erp.common.security;

import by.vs.erp.crm.entity.Client;
import by.vs.erp.crm.entity.Vehicle;
import by.vs.erp.crm.repository.VehicleRepository;
import by.vs.erp.order.entity.Booking;
import by.vs.erp.order.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class BookingSecurityServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private BookingSecurityService bookingSecurityService;

    private Authentication authenticationFor(String userId) {
        UserPrincipal principal = new UserPrincipal(userId, "client-username");
        return new UsernamePasswordAuthenticationToken(principal, "token", List.of());
    }

    private Booking bookingOwnedBy(Long ownerClientId) {
        Client client = new Client();
        client.setId(ownerClientId);

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(client);

        Booking booking = new Booking();
        booking.setVehicle(vehicle);
        return booking;
    }

    private Vehicle vehicleOwnedBy(Long ownerClientId) {
        Client client = new Client();
        client.setId(ownerClientId);

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(client);
        return vehicle;
    }

    @Test
    @DisplayName("isBookingOwner: true, когда userId из JWT совпадает с владельцем автомобиля брони")
    void isBookingOwner_MatchingOwner_ReturnsTrue() {
        Long bookingId = 10L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(bookingOwnedBy(777L)));

        boolean result = bookingSecurityService.isBookingOwner(bookingId, authenticationFor("777"));

        assertTrue(result);
    }

    @Test
    @DisplayName("isBookingOwner: false, когда бронь принадлежит другому клиенту (защита от IDOR)")
    void isBookingOwner_DifferentOwner_ReturnsFalse() {
        Long bookingId = 10L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(bookingOwnedBy(777L)));

        boolean result = bookingSecurityService.isBookingOwner(bookingId, authenticationFor("999"));

        assertFalse(result);
    }

    @Test
    @DisplayName("isBookingOwner: false, если бронь не найдена")
    void isBookingOwner_BookingNotFound_ReturnsFalse() {
        Long bookingId = 999L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        boolean result = bookingSecurityService.isBookingOwner(bookingId, authenticationFor("777"));

        assertFalse(result);
    }

    @Test
    @DisplayName("isBookingOwner: false, если principal не UserPrincipal (например, анонимная аутентификация)")
    void isBookingOwner_NonUserPrincipalAuthentication_ReturnsFalse() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());

        boolean result = bookingSecurityService.isBookingOwner(10L, authentication);

        assertFalse(result);
    }

    @Test
    @DisplayName("isBookingOwner: false при null authentication")
    void isBookingOwner_NullAuthentication_ReturnsFalse() {
        boolean result = bookingSecurityService.isBookingOwner(10L, null);

        assertFalse(result);
    }

    @Test
    @DisplayName("isVehicleOwner: true, когда userId совпадает с владельцем автомобиля")
    void isVehicleOwner_MatchingOwner_ReturnsTrue() {
        Long vehicleId = 5L;
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicleOwnedBy(42L)));

        boolean result = bookingSecurityService.isVehicleOwner(vehicleId, authenticationFor("42"));

        assertTrue(result);
    }

    @Test
    @DisplayName("isVehicleOwner: false, когда автомобиль принадлежит другому клиенту")
    void isVehicleOwner_DifferentOwner_ReturnsFalse() {
        Long vehicleId = 5L;
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicleOwnedBy(42L)));

        boolean result = bookingSecurityService.isVehicleOwner(vehicleId, authenticationFor("43"));

        assertFalse(result);
    }

    @Test
    @DisplayName("isVehicleOwner: false, если vehicleId равен null")
    void isVehicleOwner_NullVehicleId_ReturnsFalse() {
        boolean result = bookingSecurityService.isVehicleOwner(null, authenticationFor("42"));

        assertFalse(result);
    }
}