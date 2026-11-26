# Đặc tả Luxury Hotel

Ứng dụng monolith Java 21 / Spring Boot / Thymeleaf / MariaDB, một khách sạn, tiền VND.

| Mã | Yêu cầu |
|---|---|
| AUTH-01 | Đăng ký chỉ CUSTOMER, email chuẩn hóa và duy nhất, BCrypt, session và CSRF. |
| AUTH-02 | Phân quyền CUSTOMER/STAFF/ADMIN ở server; kiểm tra sở hữu đơn/hóa đơn; tài khoản khóa mất quyền ngay. |
| AUTH-03 | Xem/cập nhật hồ sơ; đăng xuất bằng POST. |
| ROOM-01 | Danh mục loại phòng, tiện nghi, giá, phòng và trạng thái vận hành riêng. |
| ROOM-02 | Tìm theo khoảng [nhận,trả), sức chứa, loại; không nhận đặt mới khi bảo trì/ngừng kinh doanh. |
| ROOM-03 | Khóa phòng trong transaction trước khi kiểm tra lịch hoặc thay đổi điều kiện kinh doanh. |
| BOOK-01 | Một phòng/đơn, mã duy nhất, lưu giá, liên hệ, số khách và số đêm. |
| BOOK-02 | PENDING giữ lịch; xác nhận/từ chối có lý do; hủy trước 14:00 ngày nhận; không khôi phục đơn kết thúc. |
| BOOK-03 | Lịch sử mọi chuyển trạng thái, tìm kiếm, lọc và phân trang. |
| STAY-01 | Nhận phòng CONFIRMED từ 14:00 đến 23:59 đúng ngày nhận, chỉ phòng READY và không có khách đang ở. Demo có cờ cho phép trước 14:00, vẫn đúng ngày nhận và giữ mọi điều kiện khác. |
| STAY-02 | NO_SHOW chỉ sau 23:59 ngày nhận; ghi dịch vụ chỉ CHECKED_IN, lưu giá và thời điểm. |
| STAY-03 | STAFF/ADMIN trả đơn CHECKED_IN bất cứ lúc nào sau nhận thực tế, kể cả thời điểm bằng nhau. Tính đủ đêm đã đặt và dịch vụ theo giá đã lưu; không giảm/hoàn/phí trả sớm. Giữ ngày dự kiến, lưu riêng giờ thực tế, người thực hiện, lý do tùy chọn. |
| STAY-04 | Preview số tiền/chính sách trước xác nhận; POST khóa phòng và đơn, tính lại server, một hóa đơn, AWAITING_PAYMENT, DIRTY, lịch còn lại được giải phóng. Request lặp không thay đổi phòng đang có khách mới. |
| STAY-05 | Dọn xong mới chuyển READY; cả lễ tân và quản trị phải kiểm tra không còn khách CHECKED_IN. Không thêm/sửa dịch vụ sau chốt. |
| BILL-01 | BigDecimal/DECIMAL; hóa đơn duy nhất/đơn, dòng chốt bất biến tại checkout. |
| BILL-02 | Thanh toán mô phỏng lưu mọi lần thất bại; thành công tạo đúng một khoản thu/hoá đơn; khóa transaction + unique. |
| BILL-03 | Doanh thu theo ngày thanh toán thành công; tách doanh thu đơn demo. |
| ADMIN-01 | Tạo/sửa/ngừng hoạt động danh mục; không xóa dữ liệu tham chiếu. |
| ADMIN-02 | Phân vai trò/khóa tài khoản; khóa tuần tự thao tác bảo vệ admin hoạt động cuối cùng. |
| ADMIN-03 | Tổng quan hôm nay, doanh thu theo khoảng ngày, hóa đơn, thanh toán và nhật ký. |

Ngày nghiệp vụ: Asia/Ho_Chi_Minh. PENDING chưa tự hết hạn. Không đổi phòng/ngày, nhận trước ngày dự kiến, gia hạn, cọc, giảm giá, thuế tách riêng, phụ thu hoặc ngân hàng thật. Giờ trả dự kiến 12:00; trả trước mốc này là trả sớm và vẫn tính đủ số đêm đã đặt. Trả muộn chỉ cảnh báo, chưa phụ thu. Trả sớm áp dụng cả ngoài demo.

Ngoại lệ nhận trước 14:00 cần profile `demo` và `hotel.demo.allow-early-check-in=true`. Ngoài demo mặc định false và service kiểm tra cả profile. Không đổi giờ hệ điều hành/database. Seed `DEMO-TODAY-yyyy-MM-dd` chỉ thêm khi thiếu và có phòng READY phù hợp; không sửa đơn thật/đơn cũ.

## Quyết định dữ liệu

- `rooms` là mutex của lịch đặt. Mọi tạo đơn/thao tác lưu trú/quản trị phòng đều khóa phòng trước, rồi khóa đơn nếu cần. Isolation READ_COMMITTED tránh đọc snapshot cũ sau khi đợi khóa. Đơn PENDING/CONFIRMED/CHECKED_IN giữ lịch. Truy vấn giao nhau dùng `check_in < newOut AND check_out > newIn`.
- Khóa riêng `system_locks` tuần tự việc đổi vai trò/khóa tài khoản và cập nhật danh mục; khi cần khóa phòng theo thứ tự id để tránh deadlock. Không có API khôi phục đơn kết thúc.
- Giá phòng nằm trong `bookings`, giá dịch vụ trong `booking_services`, dòng hóa đơn chụp cả mô tả, số lượng và giá. Danh mục sửa sau đó không tác động lịch sử.
- Checkout khóa phòng/đơn và tạo `invoices.booking_id UNIQUE`; thanh toán khóa cùng đơn, `payments.invoice_id UNIQUE`. Chỉ khoản thu thành công được dùng để tính doanh thu.
- V2 bổ sung `bookings.checked_out_by_id`, `checkout_reason` nullable bằng ALTER TABLE; giữ nguyên V1. Không backfill thời điểm hay người thực hiện cho dữ liệu cũ. CHECKED_IN thiếu thời điểm nhận thực tế phải được kiểm tra dữ liệu, không tự suy ra.
- Mọi dữ liệu nghiệp vụ dùng khóa ngoại, CHECK và index; danh mục dùng cờ active, không cung cấp xóa cứng.
- MariaDB XAMPP thực tế: 10.4.28. Dùng `hibernate-community-dialects/MariaDBLegacyDialect` vì dialect chuẩn Hibernate mới yêu cầu MariaDB >=10.6. Flyway dùng module mysql và migration SQL tương thích 10.4.

## Nguồn phiên bản

- Spring Boot **4.1.2**, stable và tương thích Java 21: https://docs.spring.io/spring-boot/system-requirements.html
- Các nhánh được duy trì: https://github.com/spring-projects/spring-boot/wiki
- Hibernate legacy dialect: https://docs.hibernate.org/orm/7.1/dialect/
- Flyway MariaDB: https://documentation.red-gate.com/fd/database-version-support-143754067.html

Đây là đặc tả triển khai, không phải hồ sơ kiểm thử học phần.
