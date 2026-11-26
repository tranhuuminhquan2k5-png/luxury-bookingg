package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="room_types") @Getter @Setter
public class RoomType {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=100) private String name;
 @Column(nullable=false,length=2000) private String description;
 @Column(nullable=false) private int capacity;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal price;
 @Column(nullable=false) private boolean active = true;
 @ManyToMany(fetch=FetchType.EAGER) @JoinTable(name="room_type_amenities",joinColumns=@JoinColumn(name="room_type_id"),inverseJoinColumns=@JoinColumn(name="amenity_id")) private Set<Amenity> amenities = new LinkedHashSet<>();
}
