package by.vs.erp.common.security;

import by.vs.erp.crm.repository.VehicleRepository;
import by.vs.erp.order.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Component("bookingSecurityService")
@RequiredArgsConstructor
public class BookingSecurityService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;

    public boolean isBookingOwner(Long bookingId, Authentication authentication) {
        String userId = extractUserId(authentication);
        if (userId == null || bookingId == null) return false;
        return bookingRepository.findById(bookingId)
                .map(booking -> ownerMatches(booking.getVehicle().getClient().getId(), userId))
                .orElse(false);
    }

    public boolean isVehicleOwner(Long vehicleId, Authentication authentication) {
        String userId = extractUserId(authentication);
        if (userId == null || vehicleId == null) return false;
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> ownerMatches(vehicle.getClient().getId(), userId))
                .orElse(false);
    }

    private boolean ownerMatches(Long ownerId, String userId) {
        return ownerId != null && ownerId.toString().equals(userId);
    }

    private String extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.id();
    }
}