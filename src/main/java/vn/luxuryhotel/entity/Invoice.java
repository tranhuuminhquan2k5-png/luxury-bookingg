package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="invoices") @Getter @Setter
public class Invoice {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(optional=false) @JoinColumn(unique=true) private Booking booking;
 @Column(nullable=false,unique=true,length=50) private String code;
 @Column(nullable=false,length=100) private String customerName;
 @Column(nullable=false,length=190) private String customerEmail;
 @Column(nullable=false,length=20) private String roomNumber;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal roomTotal;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal serviceTotal;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal total;
 @Column(nullable=false) private boolean paid;
 @Column(nullable=false) private LocalDateTime issuedAt;
}
