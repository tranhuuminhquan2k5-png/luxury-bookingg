# Luxury Hotel – Hệ thống quản lý khách sạn

## Cách chạy trên máy hiện tại — cập nhật 17/11/2026

Máy hiện có MySQL 8.0 ở cổng 3306 và MariaDB của dự án trạm sạc ở cổng 3308. Hai cổng này không phải MariaDB chứa dữ liệu Luxury Hotel đã tạo trước đó.

Trong Git Bash tại thư mục dự án, chạy:

```bash
unset DB_URL
./run-demo.cmd
```

Launcher kiểm tra/khởi động MariaDB XAMPP ở **127.0.0.1:3309**, dùng dữ liệu hiện có tại `C:\xampp\mysql\data`. Script xác minh đúng thư mục dữ liệu và database `luxury_hotel` trước khi chạy ứng dụng; không tạo lại hoặc reset database, không dừng máy chủ khác. Không cần nhấn Start MySQL trong XAMPP Control Panel cho cách chạy này. Website vẫn ở **http://localhost:8080**. Giữ terminal mở, Ctrl+C dừng Spring Boot; MariaDB được khởi động riêng vẫn tiếp tục chạy để lần mở sau dùng lại.

Nếu đã đặt `DB_URL`, launcher tôn trọng URL đó và bỏ qua tự khởi động XAMPP. Các ví dụ bên dưới dùng **3309**, đúng với máy hiện tại. Máy khác cần thay cổng theo MariaDB thực tế của mình.

**Bản cập nhật trả phòng sớm:** STAFF/ADMIN có thể trả ngay sau khi nhận phòng, vẫn tính đủ số đêm đã đặt và dịch vụ đã ghi nhận. Xem [hướng dẫn demo liên tục](docs/DEMO.md), [bảng rà soát chức năng](docs/FUNCTIONAL_REVIEW.md) và [kết quả xác minh bản sửa](docs/EARLY_CHECKOUT_VERIFICATION.md).

Ứng dụng Java monolith cho một khách sạn, giao diện tiếng Việt, chạy bằng Spring Boot tại **http://localhost:8080**. Ba vai trò: khách hàng, lễ tân, quản trị. Dự án nằm trực tiếp trong `F:\New folder\kiemthuphanmen`; workspace ban đầu trống, không có repository hoặc `AGENTS.md` áp dụng. Không sửa dự án khác.

## Công nghệ và cấu trúc

- Java **21**, Spring Boot **4.1.2** cố định trong `pom.xml`, Maven Wrapper **3.3.4** / Maven **3.9.11**.
- Spring MVC, Thymeleaf, Spring Security session/BCrypt/CSRF, Spring Data JPA, Jakarta Validation.
- MariaDB JDBC, Flyway migration SQL, Hibernate `ddl-auto=validate`, isolation `READ_COMMITTED`.
- HTML/CSS/JavaScript và Bootstrap **5.3.8** lưu local. Không có React, Next.js hay PHP nghiệp vụ.
- XAMPP hiện tại có MariaDB **10.4.28** ở cổng **3309**. Dùng `MariaDBLegacyDialect` để tương thích bản này.

```text
src/main/java/vn/luxuryhotel/
  configuration/    Bảo mật, giờ nghiệp vụ, seed demo, bootstrap admin
  controller/       Các trang MVC và xử lý form
  dto/              Form có Bean Validation, không bind trực tiếp entity
  entity/           Dữ liệu và enum trạng thái
  exception/        Lỗi nghiệp vụ và lỗi không tìm thấy
  repository/       Spring Data repositories và truy vấn tham số hóa
  service/          Transaction, kiểm tra quyền và quy tắc nghiệp vụ
src/main/resources/
  db/migration/     Migration có phiên bản, không reset dữ liệu
  templates/        Giao diện Thymeleaf
  static/           Ảnh, CSS, JS, Bootstrap
src/test/           Kiểm tra kỹ thuật phát triển, gồm kiểm tra đồng thời trên MariaDB
scripts/            Khởi động, kiểm tra và tạo database riêng
docs/               Đặc tả, thiết kế và ghi nhận xác minh kỹ thuật
```

Các thư mục `.tools/`, `.local/`, `target/` được bỏ qua bởi Git. `.tools/jdk21` là JDK portable đã tải cho máy hiện tại, không thay đổi Java mặc định của hệ thống.

## 1. Kiểm tra môi trường

Trong PowerShell:

```powershell
java -version
$env:JAVA_HOME
```

Cần **JDK 21**, không chỉ JRE. Java trong PATH của máy hiện tại là 17; dùng JDK 21 portable của dự án:

```powershell
cd 'F:\New folder\kiemthuphanmen'
$env:JAVA_HOME = (Resolve-Path '.tools\jdk21\jdk-21.0.13+9').Path
& "$env:JAVA_HOME\bin\java.exe" -version
```

Trên máy khác, cài JDK 21 từ [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21), rồi đặt `JAVA_HOME` tới thư mục JDK, **không** tới thư mục `bin`. Không cần cài Maven toàn cục: `mvnw`/`mvnw.cmd` tải đúng phiên bản Maven. Lần đầu build cần Internet để tải Maven và các phụ thuộc từ Maven Central.

## 2. Bật MariaDB trong XAMPP

Trên máy hiện tại, dùng `run-demo.cmd`: launcher khởi động/kiểm tra MariaDB XAMPP ở 3309. Không bấm Start MySQL trong Control Panel để tránh dùng nhầm cấu hình cổng cũ. Với máy cài mới không có xung đột cổng, có thể dùng Control Panel để bật MySQL/MariaDB và đặt `DB_URL` theo cổng thực tế. Chỉ bật Apache nếu cần phpMyAdmin; không cần Apache hoặc Tomcat XAMPP để chạy Luxury Hotel.

Kiểm tra kết nối local (XAMPP mặc định, chưa đặt mật khẩu root):

```powershell
& 'C:\xampp\mysql\bin\mysql.exe' -h 127.0.0.1 -P 3309 -u root -e 'SELECT VERSION();'
```

Nếu root có mật khẩu, thêm `-p` để nhập tương tác, không ghi mật khẩu vào lệnh. Không đổi cấu hình hoặc dừng database của dự án khác. Nếu XAMPP của bạn dùng cổng khác, thay cổng trong `DB_URL`.

## 3. Tạo database và tài khoản kết nối

Trên máy hiện tại, hai database riêng đã được tạo. Với cài mới, chạy SQL bằng phpMyAdmin hoặc mysql client:

```sql
CREATE DATABASE IF NOT EXISTS luxury_hotel
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS luxury_hotel_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

File tương ứng: `scripts/create-databases.sql`. Không xóa database khi khởi động. Flyway tự tạo bảng; không cần import dữ liệu SQL thủ công.

Khuyến nghị tạo tài khoản riêng; thay chuỗi placeholder bằng mật khẩu của bạn trong phiên quản trị DB, không commit mật khẩu:

```sql
CREATE USER 'luxury_app'@'127.0.0.1' IDENTIFIED BY '<MAT_KHAU_CUA_BAN>';
GRANT ALL PRIVILEGES ON luxury_hotel.* TO 'luxury_app'@'127.0.0.1';
GRANT ALL PRIVILEGES ON luxury_hotel_test.* TO 'luxury_app'@'127.0.0.1';
```

Nếu user đã tồn tại, kiểm tra quyền của user hiện có thay vì chạy lại `CREATE USER` hoặc đổi mật khẩu tùy ý. Quyền trên database dành riêng cần đủ để Flyway tạo bảng/index/constraint. Không cấp quyền trên database khác.

## 4. Chạy bằng PowerShell

Cấu hình tài khoản riêng:

```powershell
cd 'F:\New folder\kiemthuphanmen'
$env:JAVA_HOME = (Resolve-Path '.tools\jdk21\jdk-21.0.13+9').Path
$env:DB_URL = 'jdbc:mariadb://127.0.0.1:3309/luxury_hotel'
$env:DB_USERNAME = 'luxury_app'
$hotelPassword = Read-Host 'Mật khẩu database' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $hotelPassword).Password
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:MAVEN_USER_HOME = Join-Path (Get-Location) '.tools\maven-home'
.\mvnw.cmd -ntp '-Dmaven.repo.local=.tools/m2' spring-boot:run
```

Riêng trên **máy local hiện tại** với XAMPP root chưa có mật khẩu, có thể dùng:

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = ''
powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1 -Demo
```

Script tự tìm JDK portable nếu chưa có `JAVA_HOME`. Dự án chỉ cho phép root/mật khẩu rỗng trong profile `demo` hoặc `integration`. Không dùng cấu hình này cho production. Mặc định server chỉ bind `127.0.0.1`.

Mở **http://localhost:8080** sau khi log thông báo `Started LuxuryHotelApplication`. `spring-boot:run` chạy ở foreground; dùng **Ctrl+C** để dừng.

Trên máy local hiện tại (XAMPP root không mật khẩu), cách nhanh nhất là mở **`run-demo.cmd`**. Launcher kiểm tra/khởi động MariaDB XAMPP ở 3309 nếu chưa đặt `DB_URL`; không cần bật MySQL bằng Control Panel. File này thiết lập cấu hình demo trong phiên riêng, dùng lại `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` nếu bạn đã đặt.

## 5. Chạy bằng Git Bash

```bash
cd '/f/New folder/kiemthuphanmen'
export JAVA_HOME="$PWD/.tools/jdk21/jdk-21.0.13+9"
export DB_URL='jdbc:mariadb://127.0.0.1:3309/luxury_hotel'
export DB_USERNAME='luxury_app'
read -rsp 'Mật khẩu database: ' DB_PASSWORD
export DB_PASSWORD
export SPRING_PROFILES_ACTIVE=demo
export MAVEN_USER_HOME="$PWD/.tools/maven-home"
bash ./mvnw -ntp -Dmaven.repo.local=.tools/m2 spring-boot:run
```

Chỉ cho local XAMPP chưa có mật khẩu, thay hai biến bằng `export DB_USERNAME=root` và `export DB_PASSWORD=''`.

`.env.example` chỉ là mẫu tham khảo. Spring Boot **không tự đọc `.env`**. Thiết lập biến môi trường trong đúng terminal dùng để chạy ứng dụng. Biến môi trường theo phiên không còn sau khi đóng terminal; cần thiết lập lại khi mở terminal mới.

## 6. Tài khoản demo và các màn hình

Seed chỉ chạy khi có profile `demo`, chỉ thêm dữ liệu thiếu, không đổi mật khẩu hoặc ghi đè dữ liệu đã tồn tại.

| Vai trò | Email | Mật khẩu demo |
|---|---|---|
| ADMIN | `admin@luxury.local` | `LuxuryDemo!2026` |
| STAFF | `staff@luxury.local` | `LuxuryDemo!2026` |
| CUSTOMER | `customer@luxury.local` | `LuxuryDemo!2026` |

Mật khẩu demo cố ý được công bố cho đồ án; trong database lưu BCrypt. Không dùng tài khoản demo khi triển khai công khai.

- Có 4 loại phòng, 20 phòng, 6 dịch vụ và 6 tiện nghi.
- `DEMO-PENDING`: yêu cầu tương lai; `DEMO-CONFIRMED`: nhận phòng hôm nay; `DEMO-STAY`: đang ở từ hôm qua, trả hôm nay. Ngày được chọn khi seed lần đầu và **không bị sửa sau khi khởi động lại**. Về sau các đơn demo có thể trở thành đơn quá hạn đúng theo dữ liệu lịch sử.
- Mỗi ngày khởi động demo, seed thêm tối đa một `DEMO-TODAY-yyyy-MM-dd` nhận hôm nay, đặt 2 đêm nếu còn phòng READY phù hợp. Chỉ thêm khi thiếu mã; không sửa đơn cũ, không khôi phục đơn đã kết thúc. Nếu hết phòng hoặc tài khoản demo bị khóa/đổi quyền, seed bỏ qua. Có thể tự đặt đơn mới qua giao diện.
- Trong lần xác minh ngày 13/11/2026, `DEMO-STAY` đã được thao tác qua giao diện đến **Hoàn tất**, hóa đơn demo **3.900.000 ₫** đã thanh toán, phòng 301 đã dọn. Dữ liệu này được giữ để bạn tra cứu; seed không đưa đơn trở về trạng thái cũ. Một đơn demo khác tạo qua trình duyệt đã được hủy.
- Khách: `/`, `/rooms`, `/bookings`, `/profile`.
- Lễ tân: `/staff`, `/staff/bookings`, `/staff/rooms`, `/staff/invoices`, `/staff/audit`.
- Admin: mọi chức năng lễ tân, `/admin/users`, `/admin/catalog/types`, `/admin/catalog/rooms`, `/admin/catalog/amenities`, `/admin/catalog/services`, `/admin/revenue`.
- Tài khoản khách mới được đăng ký công khai. Admin phân vai trò/khóa tại danh sách tài khoản. Người đăng ký không thể tự chọn vai trò.
- Hóa đơn truy cập từ chi tiết đơn hoặc danh sách hóa đơn; có chức năng in bằng trình duyệt.
- Doanh thu mặc định chỉ gồm đơn thông thường. Chọn **Chỉ đơn demo** để xem riêng các khoản thu demo. Mọi thanh toán đều mô phỏng.

Nếu chạy ngoài demo, có thể tạo quản trị viên đầu tiên bằng hai biến `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` (12–60 ký tự), rồi khởi động với tài khoản DB riêng có mật khẩu. Bootstrap chỉ thêm khi chưa có admin hoạt động, không nâng quyền tài khoản email đã tồn tại. Xóa hai biến khỏi phiên sau khi tạo. Sau đó admin tự tạo danh mục qua giao diện.

## 7. Luồng sử dụng

1. Đăng ký/đăng nhập khách, tìm phòng theo ngày/số khách/loại phòng, xem chi tiết và gửi yêu cầu. Đơn mới ở **Chờ xác nhận**, giữ lịch ngay.
2. Lễ tân mở đơn để **Xác nhận** hoặc **Từ chối** kèm lý do. Khách có thể hủy đơn của mình trước hạn.
3. Đúng ngày nhận, phòng Sẵn sàng, lễ tân đối chiếu thông tin và **Nhận phòng**. Thông thường từ 14:00 đến 23:59; profile demo có thể bật nhận trước 14:00. Không nhận đơn ngày tương lai. Sau 23:59 mới đánh dấu **Không đến**.
4. Trong lúc khách đang ở, chọn dịch vụ và số lượng. Đơn giá được lưu tại thời điểm ghi nhận.
5. Ngay sau khi nhận phòng có thể chọn **Trả phòng**, xem số tiền/chính sách, nhập lý do tùy chọn rồi **Xác nhận trả phòng & lập hóa đơn**. Hóa đơn được chốt; phòng **Chờ dọn**, đơn **Chờ thanh toán**. Ngày dự kiến giữ nguyên; thời điểm thực tế và người thực hiện được lưu riêng.
6. Mở hóa đơn, chọn mô phỏng thất bại để lưu lần thử; hoặc thành công để ghi khoản thu và chuyển đơn **Hoàn tất**. Thanh toán lặp bị từ chối.
7. Sau khi dọn, lễ tân mở Tình trạng phòng và **Xác nhận đã dọn** để phòng Sẵn sàng.

**Chính sách trả sớm:** tiền phòng = số đêm đã đặt × đơn giá đã lưu; cộng toàn bộ dịch vụ đã ghi nhận theo giá lịch sử. Không hoàn tiền, giảm tiền theo đêm thực ở hoặc phí trả sớm. Ở vài phút vẫn trả đủ tiền theo đơn. Server tính lại khi xác nhận, không nhận tổng tiền từ trình duyệt. Sau checkout không thêm/sửa dịch vụ; chỉ một hóa đơn và một khoản thu thành công. Lịch còn lại được giải phóng ngay nhưng phải dọn phòng trước lượt nhận khách tiếp theo.

**Cấu hình nhận trước 14:00:** `application-demo.properties` đặt `hotel.demo.allow-early-check-in=${HOTEL_DEMO_EARLY_CHECK_IN:true}`. Chỉ có tác dụng khi đồng thời có profile `demo` và cờ bật; ngoài demo luôn giữ giờ thông thường, kể cả đặt cờ true. Banner **Chế độ demo** hiển thị khi ngoại lệ có hiệu lực. Đặt `HOTEL_DEMO_EARLY_CHECK_IN=false` rồi khởi động lại để tắt; PowerShell: `$env:HOTEL_DEMO_EARLY_CHECK_IN='false'`, Git Bash: `export HOTEL_DEMO_EARLY_CHECK_IN=false`. Ngoại lệ chỉ thay giờ thấp nhất trong đúng ngày nhận; vẫn giữ CONFIRMED, READY, không có khách khác và phân quyền. Giới hạn 23:59 vẫn áp dụng.

Trả sớm là nghiệp vụ chính thức cả ngoài demo. Múi giờ Asia/Ho_Chi_Minh, không thay đồng hồ Windows/database. Migration V2 chỉ thêm người trả phòng/lý do nullable; dữ liệu cũ không bị gán thời điểm hay người thực hiện giả. Đơn CHECKED_IN thiếu thời điểm nhận thực tế sẽ báo lỗi để kiểm tra dữ liệu, không tự điền giờ.

## 8. Build, kiểm tra và file chạy

Kiểm tra đơn vị và đóng gói không cần DB:

```powershell
.\mvnw.cmd -B -ntp '-Dmaven.repo.local=.tools/m2' verify
```

Kiểm tra tích hợp phải dùng **database riêng `luxury_hotel_test`**, không dùng `luxury_hotel`:

```powershell
$env:DB_USERNAME = 'root' # hoặc user test đã cấp quyền
$env:DB_PASSWORD = ''   # hoặc mật khẩu qua Read-Host ở trên
$env:TEST_DB_URL = 'jdbc:mariadb://127.0.0.1:3309/luxury_hotel_test'
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1
```

Hoặc `HOTEL_IT=true` rồi chạy Maven `verify`. Test tích hợp tự bỏ qua khi thiếu biến `HOTEL_IT=true`. Mỗi lần kiểm tra thêm dữ liệu có tên duy nhất vào database test, không drop/truncate/reset dữ liệu. Không chạy song song hai Maven build trong cùng thư mục.

Sau khi build, file chạy: `target/luxury-hotel-1.0.0.jar`.

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\luxury-hotel-1.0.0.jar --spring.profiles.active=demo
```

JAR đã chứa thư viện và tài nguyên, không cần mạng/Maven để mở lại. Vẫn cần Java 21, MariaDB đang bật và biến kết nối DB. Ghi nhận lệnh đã chạy, kết quả thực tế và giới hạn xác minh nằm trong `docs/VERIFICATION.md`; không phải báo cáo kiểm thử học phần.

## 9. Tắt máy và mở lại

Nhấn Ctrl+C tại terminal ứng dụng. Sau khi mở máy, vào dự án và chạy `./run-demo.cmd` trong Git Bash hoặc `.\run-demo.cmd` trong PowerShell; launcher tự kiểm tra MariaDB ở 3309. Dữ liệu lưu trong MariaDB, không mất khi dừng Spring Boot. Seed không xóa hoặc reset dữ liệu. Chỉ chạy một bản ứng dụng trên cổng 8080.

## 10. Xử lý sự cố

| Hiện tượng | Cách xử lý |
|---|---|
| `Address already in use` | Không dừng tiến trình lạ. Xem `netstat -ano \| findstr :8080`, chọn `$env:SERVER_PORT='8081'` hoặc `scripts/start.ps1 -Demo -Port 8081`, mở `http://localhost:8081`. |
| `Connection refused` | Chạy launcher, xem log Spring Boot và kiểm tra `DB_URL` 3309 trên máy này; mở đúng `http://localhost:8080`. Apache không chạy ứng dụng Java. |
| `Access denied for user` | Kiểm tra username, password, host được cấp quyền. Không reset DB. |
| `Unknown database` | Tạo đúng `luxury_hotel` bằng SQL ở trên. |
| Không phân giải `DB_USERNAME` | Biến chưa được đặt trong terminal chạy ứng dụng. `.env` không tự nạp. |
| `JAVA_HOME` không hợp lệ / `release 21 not supported` | Đặt về JDK 21 có `bin/java.exe` và `bin/javac.exe`, không dùng Java 17 mặc định. |
| Maven tải thất bại | Kiểm tra Internet/proxy/firewall và truy cập Maven Central. Không đổi phiên bản tùy tiện hoặc bỏ kiểm tra migration. |
| Flyway checksum mismatch | Không sửa migration đã áp dụng. Tạo migration mới; sao lưu và điều tra trước khi sửa schema, không chạy clean/reset. |
| `Unknown system variable 'WSREP_ON'` khi Flyway khởi động | XAMPP 10.4 không có biến Galera này; Flyway kiểm tra khả năng hỗ trợ. Trên máy hiện tại migration vẫn thành công; xem các dòng migrate/validate tiếp theo để xác định kết quả. |
| Không nhận/trả phòng được | Xem đúng trạng thái, ngày giờ Việt Nam, phòng đã dọn chưa. Không thay trạng thái bằng SQL để vượt quy tắc. |

## 11. Sao lưu database

Tạo thư mục riêng để lưu backup; ví dụ PowerShell:

```powershell
New-Item -ItemType Directory -Force .local\backups
& 'C:\xampp\mysql\bin\mysqldump.exe' -h 127.0.0.1 -P 3309 -u luxury_app -p `
  --single-transaction --routines --triggers --default-character-set=utf8mb4 `
  --result-file='.local\backups\luxury_hotel.sql' luxury_hotel
```

Đổi tên tệp theo ngày để giữ nhiều bản. `-p` hỏi mật khẩu, không ghi vào lệnh. Backup chứa thông tin khách hàng và hash mật khẩu, không đưa vào Git. Việc khôi phục có thể ghi đè dữ liệu: thử trên database riêng và kiểm tra bản sao lưu trước; ứng dụng không tự khôi phục/xóa dữ liệu.

## 12. Quy tắc và giới hạn phiên bản đầu

- Ngày nghiệp vụ theo `Asia/Ho_Chi_Minh`; giờ nhận/trả/không đến được cấu hình trong `application.properties`.
- Một phòng/đơn, 1–365 đêm, số khách tối đa theo loại phòng. Phòng chờ dọn có thể nhận đặt nhưng chưa thể nhận khách.
- PENDING/CONFIRMED/CHECKED_IN giữ lịch `[ngày nhận, ngày trả)`, cho phép trả và nhận cùng ngày. PENDING chưa tự hết hạn.
- Không đổi phòng, đổi ngày, nhận trước ngày dự kiến, gia hạn, cọc, giảm giá, thuế tách riêng hoặc phụ thu. Trả sớm được phép và tính đủ số đêm đã đặt; nhận trước 14:00 chỉ khi bật ngoại lệ demo. Trả muộn cảnh báo và vẫn tính số đêm đã đặt.
- Thanh toán chỉ mô phỏng. Hóa đơn đã chốt không thể sửa dòng/giá qua trình duyệt.
- Giá dùng BigDecimal/DECIMAL; trạng thái đơn độc lập trạng thái phòng. Khóa phòng trong transaction chống đặt trùng; unique invoice/booking và payment/invoice chống ghi nhận trùng. Xem `docs/REQUIREMENTS.md`.
- Chưa có thanh toán ngân hàng, gửi email/SMS, đổi mật khẩu/quên mật khẩu, nhiều khách sạn hoặc vận hành production/HTTPS. Ảnh chỉ minh họa.

Nguồn xác minh phiên bản: [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html), [các nhánh được duy trì](https://github.com/spring-projects/spring-boot/wiki), [Hibernate dialects](https://docs.hibernate.org/orm/7.1/dialect/), [Flyway database support](https://documentation.red-gate.com/fd/database-version-support-143754067.html).
