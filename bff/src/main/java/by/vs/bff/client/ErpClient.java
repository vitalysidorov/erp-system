package by.vs.bff.client;

import by.vs.bff.dto.ClientBookingResponse;
import by.vs.bff.dto.ErpBookingRequest;
import by.vs.bff.dto.TimeSlotDto;
import by.vs.erp.employee.dto.JwtResponse;
import by.vs.erp.employee.dto.LoginRequest;
import by.vs.erp.employee.dto.RefreshRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.List;

@HttpExchange("/api/v1")
public interface ErpClient {

    @PostExchange("/auth/login")
    Mono<JwtResponse> loginInErp(@RequestBody LoginRequest loginRequest);

    @PostExchange("/auth/refresh")
    Mono<JwtResponse> refreshInErp(@RequestBody RefreshRequest refreshRequest);

    @PostExchange("/clients/register")
    Mono<Object> registerClientInErp(@RequestBody Object clientRegisterRequest);

    @GetExchange("/bookings/suggest-slots")
    Mono<List<TimeSlotDto>> getAvailableSlots(
            @RequestParam("date") LocalDateTime date,
            @RequestParam("durationHours") int durationHours
    );

    @PostExchange("/bookings")
    Mono<ClientBookingResponse> createBooking(@RequestBody ErpBookingRequest erpBookingRequest);

    @PatchExchange("/bookings/{bookingId}/cancel")
    Mono<Void> cancelBooking(@PathVariable("bookingId") Long bookingId);
}
