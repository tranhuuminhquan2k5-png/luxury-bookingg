package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="services") @Getter @Setter
public class HotelService {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=100) private String name;
 @Column(nullable=false,length=40) private String unit;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal price;
 @Column(nullable=false) private boolean active = true;
}
