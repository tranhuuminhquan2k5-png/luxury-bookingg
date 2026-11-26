package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="invoice_items") @Getter @Setter
public class InvoiceItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) private Invoice invoice;
 @Column(nullable=false,length=200) private String description;
 @Column(nullable=false) private long quantity;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal unitPrice;
 @Column(nullable=false,precision=16,scale=2) private BigDecimal total;
}
