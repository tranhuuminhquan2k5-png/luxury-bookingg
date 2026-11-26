package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="booking_status_history") @Getter @Setter
public class BookingHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) private Booking booking;
 @Column(length=30) private String fromStatus;
 @Column(nullable=false,length=30) private String toStatus;
 @ManyToOne(optional=false) private UserAccount actor;
 @Column(length=1000) private String reason;
 @Column(nullable=false) private LocalDateTime createdAt;
}
