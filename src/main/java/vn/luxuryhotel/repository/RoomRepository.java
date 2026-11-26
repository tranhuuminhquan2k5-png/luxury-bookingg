package vn.luxuryhotel.repository;
import vn.luxuryhotel.entity.Room;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface RoomRepository extends JpaRepository<Room,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Room r where r.id=:id")
    Optional<Room> lockById(Long id);
    Optional<Room> findByNumber(String number);
}
