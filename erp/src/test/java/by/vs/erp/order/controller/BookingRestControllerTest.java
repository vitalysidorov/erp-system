package by.vs.erp.order.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.common.security.BookingSecurityService;
import by.vs.erp.order.dto.BookingRequestDto;
import by.vs.erp.order.dto.BookingResponseDto;
import by.vs.erp.order.dto.TimeSlotDto;
import by.vs.erp.order.service.BookingService;
import by.vs.erp.order.service.SmartBookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingRestControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private SmartBookingService smartBookingService;

    @MockBean(name = "bookingSecurityService")
    private BookingSecurityService bookingSecurityService;

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /bookings: Успешное создание бронирования менеджером с возвратом BookingResponseDto")
    void shouldCreateBookingWhenUserIsManager() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime end = start.plusHours(2);
        BookingRequestDto dto = new BookingRequestDto(1L, start, end, "manager", "MANUAL");

        BookingResponseDto responseDto = BookingResponseDto.builder()
                .id(55L)
                .createdBy("manager")
                .source("MANUAL")
                .vehicleId(1L)
                .startTime(start)
                .endTime(end)
                .status("CONFIRMED")
                .build();

        Mockito.when(bookingService.createBooking(any(BookingRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55L))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.createdBy").value("manager"))
                .andExpect(jsonPath("$.vehicleId").value(1L));

        Mockito.verifyNoInteractions(bookingSecurityService);
    }

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("POST /bookings: Отказ в доступе (403) для роли MASTER")
    void shouldReturnForbiddenWhenUserIsMaster() throws Exception {
        BookingRequestDto dto = new BookingRequestDto(1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1), "client", "BFF_WEB");

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    @DisplayName("POST /bookings: CLIENT создающий бронь на СВОЙ автомобиль получает 201")
    void shouldCreateBookingWhenClientOwnsVehicle() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime end = start.plusHours(2);
        BookingRequestDto dto = new BookingRequestDto(7L, start, end, null, "BFF_WEB");

        BookingResponseDto responseDto = BookingResponseDto.builder()
                .id(60L)
                .vehicleId(7L)
                .startTime(start)
                .endTime(end)
                .status("CONFIRMED")
                .build();

        Mockito.when(bookingSecurityService.isVehicleOwner(eq(7L), any())).thenReturn(true);
        Mockito.when(bookingService.createBooking(any(BookingRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(60L));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    @DisplayName("POST /bookings: CLIENT пытающийся создать бронь на ЧУЖОЙ автомобиль получает 403 (закрытие IDOR)")
    void shouldReturnForbiddenWhenClientDoesNotOwnVehicle() throws Exception {
        BookingRequestDto dto = new BookingRequestDto(
                999L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2), null, "BFF_WEB"
        );

        Mockito.when(bookingSecurityService.isVehicleOwner(eq(999L), any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        Mockito.verify(bookingService, Mockito.never()).createBooking(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /suggest-slots: Успешное получение списка свободных окон для записи")
    void shouldSuggestFreeSlots() throws Exception {
        LocalDateTime targetDate = LocalDateTime.of(2026, 9, 1, 10, 0);
        TimeSlotDto slot = new TimeSlotDto(targetDate, targetDate.plusHours(2));

        Mockito.when(smartBookingService.findAvailableSlots(eq(targetDate), eq(2)))
                .thenReturn(List.of(slot));

        mockMvc.perform(get("/api/v1/bookings/suggest-slots")
                        .param("date", "2026-09-01T10:00:00")
                        .param("durationHours", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotStart").exists())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("PATCH /id/cancel: MANAGER может отменить любую бронь (без проверки владения)")
    void shouldCancelBookingWhenUserIsManager() throws Exception {
        Long bookingId = 10L;

        mockMvc.perform(patch("/api/v1/bookings/{id}/cancel", bookingId))
                .andExpect(status().isNoContent());

        Mockito.verify(bookingService, Mockito.times(1)).cancelBooking(bookingId);
        Mockito.verifyNoInteractions(bookingSecurityService);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    @DisplayName("PATCH /id/cancel: CLIENT может отменить СВОЮ бронь")
    void shouldCancelBookingWhenClientOwnsBooking() throws Exception {
        Long bookingId = 11L;
        Mockito.when(bookingSecurityService.isBookingOwner(eq(bookingId), any())).thenReturn(true);

        mockMvc.perform(patch("/api/v1/bookings/{id}/cancel", bookingId))
                .andExpect(status().isNoContent());

        Mockito.verify(bookingService, Mockito.times(1)).cancelBooking(bookingId);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    @DisplayName("PATCH /id/cancel: CLIENT пытающийся отменить ЧУЖУЮ бронь получает 403 и запрос не доходит до сервиса")
    void shouldReturnForbiddenWhenClientCancelsSomeoneElsesBooking() throws Exception {
        Long bookingId = 12L;
        Mockito.when(bookingSecurityService.isBookingOwner(eq(bookingId), any())).thenReturn(false);

        mockMvc.perform(patch("/api/v1/bookings/{id}/cancel", bookingId))
                .andExpect(status().isForbidden());

        Mockito.verify(bookingService, Mockito.never()).cancelBooking(any());
    }
}