package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="bookings") @Getter @Setter
public class Booking {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=50) private String code;
 @ManyToOne(optional=false) private UserAccount customer;
 @ManyToOne(optional=false) private Room room;
 @Column(nullable=false) private LocalDate checkIn;
 @Column(nullable=false) private LocalDate checkOut;
 @Column(nullable=false) private int guests;
 @Column(nullable=false) private long nights;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal nightlyPrice;
 @Column(nullable=false,length=100) private String contactName;
 @Column(nullable=false,length=20) private String contactPhone;
 @Column(nullable=false,length=190) private String contactEmail;
 @Column(length=1000) private String note;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private BookingStatus status;
 @Column(nullable=false) private boolean demo;
 @Column(nullable=false) private LocalDateTime createdAt;
 private LocalDateTime checkedInAt;
 private LocalDateTime checkedOutAt;
 @ManyToOne @JoinColumn(name="checked_out_by_id") private UserAccount checkedOutBy;
 @Column(length=500) private String checkoutReason;
 public BigDecimal getRoomTotal() { return nightlyPrice.multiply(BigDecimal.valueOf(nights)); }
}
