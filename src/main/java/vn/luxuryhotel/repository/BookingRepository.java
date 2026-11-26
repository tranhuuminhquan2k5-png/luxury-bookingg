package vn.luxuryhotel.repository;
import vn.luxuryhotel.entity.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
public interface BookingRepository extends JpaRepository<Booking,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from Booking b where b.id=:id")
    Optional<Booking> lockById(Long id);
    @Query("select count(b) from Booking b where b.room.id=:room and b.status in :statuses and b.checkIn < :outDate and b.checkOut > :inDate")
    long overlaps(Long room, LocalDate inDate, LocalDate outDate, Collection<BookingStatus> statuses);
    List<Booking> findByRoomIdAndStatusIn(Long room,Collection<BookingStatus> statuses);
    boolean existsByCode(String code);
}
