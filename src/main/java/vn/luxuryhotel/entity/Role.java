package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="roles") @Getter @Setter
public class Role {
 
 @Id @Column(length=20) private String name;
}
