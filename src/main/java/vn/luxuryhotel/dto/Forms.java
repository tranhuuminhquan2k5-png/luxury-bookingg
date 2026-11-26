package vn.luxuryhotel.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;

public final class Forms {
    private Forms() {}
    @Getter @Setter public static class Register {
        @NotBlank(message="Nhập họ tên.") @Size(max=100) private String fullName;
        @NotBlank @Email(message="Email không hợp lệ.") @Size(max=190) private String email;
        @NotBlank @Pattern(regexp="[+0-9 ()-]{8,20}",message="Số điện thoại cần 8–20 ký tự hợp lệ.") private String phone;
        @NotBlank @Size(min=10,max=60,message="Mật khẩu cần 10–60 ký tự.") private String password;
        @AssertTrue(message="Mật khẩu tối đa 72 byte UTF-8 để băm BCrypt an toàn.")
        public boolean isPasswordWithinBcryptLimit(){return password==null||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72;}
    }
    @Getter @Setter public static class Profile {
        @NotBlank @Size(max=100) private String fullName;
        @NotBlank @Pattern(regexp="[+0-9 ()-]{8,20}",message="Số điện thoại không hợp lệ.") private String phone;
        @Size(max=250) private String address;
    }
    @Getter @Setter public static class Search {
        @NotNull(message="Chọn ngày nhận.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate checkIn;
        @NotNull(message="Chọn ngày trả.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate checkOut;
        @Min(value=1,message="Cần ít nhất 1 khách.") @Max(100) private int guests=2;
        private Long typeId;
    }
    @Getter @Setter public static class Book {
        @NotNull private Long roomId;
        @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate checkIn;
        @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate checkOut;
        @Min(1) @Max(100) private int guests=2;
        @NotBlank @Size(max=100) private String contactName;
        @NotBlank @Pattern(regexp="[+0-9 ()-]{8,20}",message="Số điện thoại không hợp lệ.") private String contactPhone;
        @NotBlank @Email @Size(max=190) private String contactEmail;
        @Size(max=1000) private String note;
    }
    @Getter @Setter public static class Type {
        private Long id;
        @NotBlank @Size(max=100) private String name;
        @NotBlank @Size(max=2000) private String description;
        @Min(1) @Max(100) private int capacity=2;
        @NotNull @DecimalMin("1") @Digits(integer=12,fraction=2) private BigDecimal price;
        private boolean active=true;
        private Set<Long> amenityIds=new LinkedHashSet<>();
    }
    @Getter @Setter public static class Room {
        private Long id;
        @NotBlank @Pattern(regexp="[A-Za-z0-9-]{1,20}",message="Số phòng gồm chữ, số hoặc dấu gạch ngang.") private String number;
        @Min(-5) @Max(200) private int floor=1;
        @NotNull private Long typeId;
        @NotBlank private String status="READY";
    }
    @Getter @Setter public static class Amenity {
        private Long id;
        @NotBlank @Size(max=100) private String name;
        private boolean active=true;
    }
    @Getter @Setter public static class Service {
        private Long id;
        @NotBlank @Size(max=100) private String name;
        @NotBlank @Size(max=40) private String unit;
        @NotNull @DecimalMin("1") @Digits(integer=12,fraction=2) private BigDecimal price;
        private boolean active=true;
    }
    @Getter @Setter public static class UserEdit {
        @NotNull private Long id;
        @NotBlank @Pattern(regexp="CUSTOMER|STAFF|ADMIN") private String role;
        private boolean enabled;
    }
    @Getter @Setter public static class UseService {
        @NotNull private Long serviceId;
        @Min(1) @Max(10000) private int quantity=1;
    }
}
