package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="system_locks") @Getter @Setter
public class SystemLock {
 
 @Id private Long id;
}
