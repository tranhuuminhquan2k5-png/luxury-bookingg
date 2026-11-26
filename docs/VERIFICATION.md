# Ghi nhận xác minh kỹ thuật

Kết quả mới nhất của chức năng trả sớm nằm tại [EARLY_CHECKOUT_VERIFICATION.md](EARLY_CHECKOUT_VERIFICATION.md), kèm [bảng rà soát chức năng](FUNCTIONAL_REVIEW.md). Nội dung dưới đây giữ làm lịch sử các lần xác minh trước.

## Sửa khởi động ngày 17/11/2026

Log người dùng gửi có lỗi `Access denied for user 'root'@'localhost' (using password: NO)`. Kiểm tra xác định cổng 3306 là dịch vụ MySQL80 với dữ liệu tại `C:\ProgramData\MySQL\MySQL Server 8.0\Data`; cổng 3308 là MariaDB dùng thư mục `pttkht/artifacts/charging-test-db-20261031`, thuộc dự án khác. Dữ liệu Luxury Hotel cũ nằm tại `C:\xampp\mysql\data`.

Đã bổ sung `scripts/start-xampp-db.ps1` và cập nhật launcher demo: khi không có DB_URL, khởi động/kiểm tra XAMPP MariaDB trên cổng riêng 3309, xác minh `@@datadir` và database `luxury_hotel` trước khi chạy ứng dụng. Không đổi `my.ini`, không reset/copy dữ liệu, không dừng hai máy chủ khác. Dùng dữ liệu cũ đọc được 5 đơn và 1 hóa đơn.

Chạy chính lệnh Git Bash `unset DB_URL; ./run-demo.cmd` thành công; log có `Started LuxuryHotelApplication`. GET `http://127.0.0.1:8080/` trả HTTP 200 và có nội dung Luxury Hotel. Thay đổi chỉ ở launcher, đã kiểm tra cú pháp PowerShell và khởi động thực tế; không chạy lại bộ test nghiệp vụ.

Các lần chạy dưới đây thực hiện ngày **13/11/2026**, giờ Việt Nam, trong quá trình phát triển. Đây không phải hồ sơ kiểm thử học phần, bảng test case hoặc tuyên bố độ bao phủ.

## Môi trường đã chạy

- Windows 11, JDK Eclipse Temurin 21.0.13 portable tại `.tools/jdk21/jdk-21.0.13+9`.
- Maven Wrapper 3.3.4, Maven 3.9.11, Spring Boot 4.1.2.
- MariaDB XAMPP 10.4.28 tại 127.0.0.1:3306.
- Database ứng dụng `luxury_hotel`; database kiểm tra riêng `luxury_hotel_test`, charset utf8mb4.
- Chrome có sẵn trên máy, điều khiển bằng Playwright Core cho kiểm tra giao diện.

## Lệnh và kết quả quan sát

1. `mvnw.cmd -B -ntp -Dmaven.repo.local=.tools/m2 -DskipTests compile`: BUILD SUCCESS với Java 21.
2. `mvnw.cmd -B -ntp -Dmaven.repo.local=.tools/m2 spring-boot:run`, profile demo: khởi động thành công trên localhost:8080. Flyway áp dụng migration V1, Hibernate validate schema thành công. Có cảnh báo dò biến Galera `WSREP_ON` trên XAMPP; migration và khởi động vẫn thành công.
3. Với `HOTEL_IT=true`, `mvnw.cmd -B -ntp -Dmaven.repo.local=.tools/m2 verify`: **9 kiểm tra, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS. Các kiểm tra xác minh luồng lưu trú, giá lịch sử, hóa đơn, lần thanh toán thất bại/thành công, đặt phòng đồng thời, thanh toán đồng thời, ngày giờ, sức chứa, quyền sở hữu, CSRF, ba vai trò, bảo vệ admin cuối cùng, trạng thái phòng và biểu mẫu quản trị.
4. Sau sửa fragment điều hướng, chạy lại `mvnw.cmd -B -ntp -Dmaven.repo.local=.tools/m2 -Dtest=HotelIntegrationTest#authenticationAuthorizationOwnershipCsrfAndAllPageTemplates verify`: **1 kiểm tra, 0 failures/errors**, BUILD SUCCESS. JAR cuối cùng là `target/luxury-hotel-1.0.0.jar`.
5. `bash ./mvnw -v`: Maven 3.9.11 và Java 21.0.13 hoạt động qua Git Bash.
6. `java -jar target/luxury-hotel-1.0.0.jar --spring.profiles.active=demo --debug=false`: khởi động thành công, trang chủ trả HTTP 200.
7. `node .tools/browser-check.cjs`: kiểm tra Chrome thành công với giao diện desktop/mobile, ảnh local, đăng nhập/đăng xuất ba vai trò, tìm phòng → xem chi tiết → gửi yêu cầu → hủy, và chỉnh sửa bốn danh mục. Không ghi nhận lỗi JavaScript. Lỗi định dạng ngày và tràn ngang mobile phát hiện trước đó đã được sửa và kiểm tra lại.
8. Qua giao diện lễ tân, đơn `DEMO-STAY` đã được ghi dịch vụ, trả phòng, thanh toán thất bại rồi thành công và dọn phòng. Đối chiếu database: tiền phòng 3.200.000 VND + dịch vụ 700.000 VND = **3.900.000 VND**; hai lần thử, đúng một khoản thu; đơn COMPLETED, phòng 301 READY. Khoản thu thuộc dữ liệu demo.
9. Sau dừng và chạy lại JAR, so sánh dữ liệu đặt phòng, hóa đơn, khoản thu, phòng và số lượng tài khoản/loại phòng trước–sau: **không thay đổi**. Seed không tạo trùng. `node .tools/browser-final.cjs` xác nhận hóa đơn đã thanh toán vẫn xem được, điều hướng lễ tân không lặp, trang chủ và hóa đơn không tràn ngang trên mobile.

Log phát triển và ảnh kiểm tra được lưu trong `.tools/` (không đưa vào Git). Maven tạo kết quả kiểm tra trong `target/surefire-reports`; mỗi lần chạy có thể ghi đè kết quả lần trước. Test chỉ thêm dữ liệu riêng vào database test, không drop/truncate/reset dữ liệu.

## Giới hạn xác minh

- Chưa kiểm tra trên máy khác, Safari/Firefox, môi trường production hoặc tải lớn kéo dài. Kiểm tra đồng thời đã thực hiện với hai yêu cầu cạnh tranh.
- Chưa thử phục hồi bản sao lưu vào một máy khác.
- Các quy tắc thời gian xuyên nhiều ngày được kiểm tra bằng Clock riêng trong integration test; không thay đổi giờ hệ thống hoặc thêm đường tắt vào ứng dụng.
- Không có ngân hàng thật, email/SMS hoặc HTTPS production. Phạm vi chưa hỗ trợ được ghi trong README và trên giao diện.
- Dữ liệu demo đã thao tác được giữ nguyên; ngày seed không tự cập nhật khi mở lại vào ngày khác.
