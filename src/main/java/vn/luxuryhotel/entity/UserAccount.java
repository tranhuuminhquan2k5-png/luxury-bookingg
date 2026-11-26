package vn.luxuryhotel.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="users") @Getter @Setter
public class UserAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=190) private String email;
 @Column(nullable=false) private String password;
 @Column(nullable=false,length=100) private String fullName;
 @Column(nullable=false,length=20) private String phone;
 @Column(length=250) private String address;
 @ManyToOne(optional=false) @JoinColumn(name="role_name") private Role role;
 @Column(nullable=false) private boolean enabled = true;
 @Column(nullable=false) private boolean demo;
 @Column(nullable=false) private LocalDateTime createdAt;
}
