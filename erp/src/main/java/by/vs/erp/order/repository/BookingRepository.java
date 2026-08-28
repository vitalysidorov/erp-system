package by.vs.erp.order.repository;

import by.vs.erp.order.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId AND b.status = 'CONFIRMED' " +
            "AND (:start < b.endTime AND :end > b.startTime)")
    List<Booking> findOverlappingBookingsWithLock(@Param("vehicleId") Long vehicleId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
            "AND (:start < b.endTime AND :end > b.startTime) ORDER BY b.startTime ASC")
    List<Booking> findOverlappingBookings(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}
