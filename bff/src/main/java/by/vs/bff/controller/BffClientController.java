package by.vs.bff.controller;

import by.vs.bff.dto.*;
import by.vs.bff.service.BffBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.List;

@RestController
@RequestMapping("/api/v1/client/bookings")
@RequiredArgsConstructor
@Tag(name = "Client Booking Controller", description = "Управление бронированием со стороны B2C клиентов")
public class BffClientController {

    private final BffBookingService bffService;

    @GetMapping("/slots")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Получить свободные временные слоты",
            description = "Запрашивает у ERP сетку свободных постов на указанную дату с фильтрацией по длительности.")
    public Mono<List<TimeSlotDto>> getSlots(@RequestParam String date, @RequestParam int durationHours) {
        return bffService.getSlotsForClient(date, durationHours);
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Создать новое бронирование")
    public Mono<ClientBookingResponse> create(@RequestBody ClientBookingRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        String clientId = jwt.getClaimAsString("userId");
        return bffService.createBookingForClient(clientId, request);
    }


    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Отменить бронь",
            description = "Пробрасывает в ERP запрос на отмену бронирования.")
    public Mono<Void> cancel(@PathVariable Long id) {
        return bffService.cancelBookingByClient(id);
    }
}
