package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="booking_services") @Getter @Setter
public class BookingServiceItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) private Booking booking;
 @ManyToOne(optional=false) private HotelService service;
 @Column(nullable=false,length=150) private String description;
 @Column(nullable=false) private int quantity;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal unitPrice;
 @Column(nullable=false) private LocalDateTime usedAt;
 @ManyToOne(optional=false) private UserAccount actor;
 public BigDecimal getTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
