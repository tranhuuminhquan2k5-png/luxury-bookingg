CREATE TABLE roles (name VARCHAR(20) PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO roles(name) VALUES ('CUSTOMER'),('STAFF'),('ADMIN');
CREATE TABLE system_locks (id BIGINT PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO system_locks(id) VALUES (1);
CREATE TABLE users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(190) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL, full_name VARCHAR(100) NOT NULL, phone VARCHAR(20) NOT NULL,
 address VARCHAR(250), role_name VARCHAR(20) NOT NULL, enabled BIT NOT NULL DEFAULT 1,
 demo BIT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_user_role FOREIGN KEY(role_name) REFERENCES roles(name)
) ENGINE=InnoDB;
CREATE TABLE amenities (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, active BIT NOT NULL DEFAULT 1) ENGINE=InnoDB;
CREATE TABLE room_types (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, description VARCHAR(2000) NOT NULL,
 capacity INT NOT NULL CHECK(capacity > 0), price DECIMAL(16,2) NOT NULL CHECK(price > 0), active BIT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
CREATE TABLE room_type_amenities (
 room_type_id BIGINT NOT NULL, amenity_id BIGINT NOT NULL, PRIMARY KEY(room_type_id,amenity_id),
 FOREIGN KEY(room_type_id) REFERENCES room_types(id), FOREIGN KEY(amenity_id) REFERENCES amenities(id)
) ENGINE=InnoDB;
CREATE TABLE rooms (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, number VARCHAR(20) NOT NULL UNIQUE, floor INT NOT NULL,
 room_type_id BIGINT NOT NULL, status VARCHAR(30) NOT NULL,
 FOREIGN KEY(room_type_id) REFERENCES room_types(id),
 CONSTRAINT ck_room_status CHECK(status IN ('READY','OCCUPIED','DIRTY','MAINTENANCE','INACTIVE'))
) ENGINE=InnoDB;
CREATE TABLE bookings (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE,
 customer_id BIGINT NOT NULL, room_id BIGINT NOT NULL, check_in DATE NOT NULL, check_out DATE NOT NULL,
 guests INT NOT NULL CHECK(guests > 0), nights BIGINT NOT NULL CHECK(nights > 0),
 nightly_price DECIMAL(16,2) NOT NULL CHECK(nightly_price > 0), contact_name VARCHAR(100) NOT NULL,
 contact_phone VARCHAR(20) NOT NULL, contact_email VARCHAR(190) NOT NULL, note VARCHAR(1000),
 status VARCHAR(30) NOT NULL, demo BIT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL,
 checked_in_at DATETIME(6), checked_out_at DATETIME(6),
 FOREIGN KEY(customer_id) REFERENCES users(id), FOREIGN KEY(room_id) REFERENCES rooms(id),
 CONSTRAINT ck_booking_dates CHECK(check_out > check_in),
 CONSTRAINT ck_booking_status CHECK(status IN ('PENDING','CONFIRMED','CHECKED_IN','CANCELLED','REJECTED','NO_SHOW','AWAITING_PAYMENT','COMPLETED')),
 INDEX idx_booking_overlap(room_id,status,check_in,check_out), INDEX idx_booking_customer(customer_id,created_at),
 INDEX idx_booking_arrivals(check_in,status), INDEX idx_booking_departures(check_out,status)
) ENGINE=InnoDB;
CREATE TABLE booking_status_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, booking_id BIGINT NOT NULL, from_status VARCHAR(30), to_status VARCHAR(30) NOT NULL,
 actor_id BIGINT NOT NULL, reason VARCHAR(1000), created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(booking_id) REFERENCES bookings(id), FOREIGN KEY(actor_id) REFERENCES users(id), INDEX idx_history_booking(booking_id,id)
) ENGINE=InnoDB;
CREATE TABLE services (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, unit VARCHAR(40) NOT NULL,
 price DECIMAL(16,2) NOT NULL CHECK(price > 0), active BIT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
CREATE TABLE booking_services (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, booking_id BIGINT NOT NULL, service_id BIGINT NOT NULL,
 description VARCHAR(150) NOT NULL, quantity INT NOT NULL CHECK(quantity > 0), unit_price DECIMAL(16,2) NOT NULL CHECK(unit_price > 0),
 used_at DATETIME(6) NOT NULL, actor_id BIGINT NOT NULL,
 FOREIGN KEY(booking_id) REFERENCES bookings(id), FOREIGN KEY(service_id) REFERENCES services(id), FOREIGN KEY(actor_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE invoices (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, booking_id BIGINT NOT NULL UNIQUE, code VARCHAR(50) NOT NULL UNIQUE,
 customer_name VARCHAR(100) NOT NULL, customer_email VARCHAR(190) NOT NULL, room_number VARCHAR(20) NOT NULL,
 room_total DECIMAL(16,2) NOT NULL, service_total DECIMAL(16,2) NOT NULL, total DECIMAL(16,2) NOT NULL,
 paid BIT NOT NULL DEFAULT 0, issued_at DATETIME(6) NOT NULL,
 FOREIGN KEY(booking_id) REFERENCES bookings(id), CHECK(total = room_total + service_total)
) ENGINE=InnoDB;
CREATE TABLE invoice_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, invoice_id BIGINT NOT NULL, description VARCHAR(200) NOT NULL,
 quantity BIGINT NOT NULL CHECK(quantity > 0), unit_price DECIMAL(16,2) NOT NULL, total DECIMAL(16,2) NOT NULL,
 FOREIGN KEY(invoice_id) REFERENCES invoices(id), CHECK(total = quantity * unit_price)
) ENGINE=InnoDB;
CREATE TABLE payment_attempts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, invoice_id BIGINT NOT NULL, actor_id BIGINT NOT NULL,
 successful BIT NOT NULL, reference VARCHAR(50) NOT NULL, attempted_at DATETIME(6) NOT NULL,
 FOREIGN KEY(invoice_id) REFERENCES invoices(id), FOREIGN KEY(actor_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE payments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, invoice_id BIGINT NOT NULL UNIQUE, attempt_id BIGINT NOT NULL UNIQUE,
 amount DECIMAL(16,2) NOT NULL CHECK(amount > 0), paid_at DATETIME(6) NOT NULL,
 FOREIGN KEY(invoice_id) REFERENCES invoices(id), FOREIGN KEY(attempt_id) REFERENCES payment_attempts(id), INDEX idx_payments_date(paid_at)
) ENGINE=InnoDB;
CREATE TABLE audit_logs (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_id BIGINT NOT NULL, action VARCHAR(60) NOT NULL,
 detail VARCHAR(1000) NOT NULL, created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(actor_id) REFERENCES users(id), INDEX idx_audit_date(created_at)
) ENGINE=InnoDB;
