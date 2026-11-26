package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="rooms") @Getter @Setter
public class Room {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=20) private String number;
 @Column(nullable=false) private int floor;
 @ManyToOne(optional=false) private RoomType roomType;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private RoomStatus status = RoomStatus.READY;
}
