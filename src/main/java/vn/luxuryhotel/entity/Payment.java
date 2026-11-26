package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="payments") @Getter @Setter
public class Payment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(optional=false) @JoinColumn(unique=true) private Invoice invoice;
 @OneToOne(optional=false) @JoinColumn(unique=true) private PaymentAttempt attempt;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal amount;
 @Column(nullable=false) private LocalDateTime paidAt;
}
