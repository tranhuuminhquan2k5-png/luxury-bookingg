package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="payment_attempts") @Getter @Setter
public class PaymentAttempt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) private Invoice invoice;
 @ManyToOne(optional=false) private UserAccount actor;
 @Column(nullable=false) private boolean successful;
 @Column(nullable=false,length=50) private String reference;
 @Column(nullable=false) private LocalDateTime attemptedAt;
}
