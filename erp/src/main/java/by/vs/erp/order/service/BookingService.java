package by.vs.erp.order.service;

import by.vs.erp.common.exception.NotFoundException;
import by.vs.erp.common.security.UserPrincipal;
import by.vs.erp.order.dto.BookingRequestDto;
import by.vs.erp.order.dto.BookingResponseDto;
import by.vs.erp.order.entity.Booking;
import by.vs.erp.order.mapper.BookingMapper;
import by.vs.erp.order.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.id();
        }

        throw new IllegalStateException("Пользователь не аутентифицирован");
    }

    private boolean isManagerOrAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto dto) {
        log.info("Попытка создания брони для автомобиля ID: {}", dto.getVehicleId());

        List<Booking> overlapping = bookingRepository.findOverlappingBookingsWithLock(
                dto.getVehicleId(), dto.getStartTime(), dto.getEndTime()
        );

        if (!overlapping.isEmpty()) {
            log.error("Ошибка бронирования: выбранное время для автомобиля {} уже занято.", dto.getVehicleId());
            throw new IllegalStateException("Выбранный временной интервал уже занят для данного транспорта или поста.");
        }

        Booking booking = bookingMapper.toEntity(dto);

        booking.setCreatedBy(getCurrentUserId());
        booking.setSource(isManagerOrAdmin() ? "MANUAL" : "BFF_WEB");
        booking.setStatus("CONFIRMED");

        return bookingMapper.toDto(bookingRepository.save(booking));
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронь не найдена"));
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }
}