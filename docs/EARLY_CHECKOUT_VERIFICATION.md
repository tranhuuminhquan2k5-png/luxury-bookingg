# Xác minh kỹ thuật bản trả phòng sớm

Thực hiện **17/11/2026**, Asia/Ho_Chi_Minh. Đây là ghi nhận kỹ thuật phát triển, không phải báo cáo kiểm thử môn học hoặc tuyên bố kiểm thử toàn diện.

## Thay đổi đã thực hiện

Điều kiện cũ trong `BookingService.checkout` chặn `time.today().isBefore(b.getCheckOut())` đã được bỏ. Thay bằng kiểm tra CHECKED_IN, quyền STAFF/ADMIN và actual checkout >= actual checkin. Ngoại lệ nhận trước 14:00 nằm riêng trong `StayPolicy` và cần cả profile demo lẫn cờ cấu hình; trả sớm là nghiệp vụ chính thức.

- Tính tiền đủ số đêm và đơn giá đã lưu, cộng dịch vụ; không tin tổng tiền từ client.
- Preview trước xác nhận, cảnh báo trả sớm, lý do tùy chọn, actor và actual checkout.
- Khóa phòng rồi đơn, một transaction cho hóa đơn/dòng hóa đơn/trạng thái/lịch sử/room DIRTY. Request lặp không làm bẩn phòng của khách mới.
- Clean và admin READY kiểm tra không còn CHECKED_IN.
- Flash lỗi tại chi tiết đơn cho thao tác lưu trú/hủy/thanh toán, giữ phiên đăng nhập.
- Migration V2 thêm 2 cột nullable và FK; V1 giữ nguyên.
- Seed hôm nay idempotent, README/chính sách/demo/audit cập nhật.

## Môi trường và bảo toàn dữ liệu

- Windows, JDK Temurin 21.0.13 tại `.tools/jdk21/jdk-21.0.13+9`, Maven Wrapper của dự án, Spring Boot 4.1.2.
- MariaDB XAMPP 10.4.28 ở **127.0.0.1:3309**, datadir `C:\xampp\mysql\data`.
- Test: **luxury_hotel_test**. Chạy giao diện demo: **luxury_hotel**. Không viết/xóa dữ liệu EV Station hoặc database khác, không đổi đồng hồ hệ thống/DB.
- Backup trước migration: `.local/backups/luxury_hotel_before_early_checkout_20261117_205106.sql`, 27.156 byte, không đưa vào mã nguồn công khai.
- Snapshot trước sửa: `.tools/before-early-checkout.txt`, gồm các trường id/mã/trạng thái/ngày dự kiến/giờ thực tế của 6 đơn cũ và id/mã/tổng/paid của hóa đơn cũ.
- Sau migration và sau khởi động lại, `Compare-Object` hai snapshot không có khác biệt. Các cột mới của 6 đơn cũ vẫn NULL. Đơn #6 CHECKED_IN của người dùng giữ nguyên; không checkout đơn này.
- Hóa đơn cũ #1 vẫn 3.900.000đ, đã thanh toán. Chrome mở `/bookings/1` đến `/bookings/6` và `/bookings/3/invoice` trả 200.
- Seed chỉ thêm #7 `DEMO-TODAY-2026-11-17`. Đơn #8 được tạo mới qua UI để xác minh, hiện COMPLETED, phòng 104 READY. Sau restart, seed ngày này vẫn count=1.

## Lệnh đã chạy và kết quả

### 1. Backup và kiểm tra schema

Backup chỉ database Luxury Hotel bằng `C:\xampp\mysql\bin\mysqldump.exe`, cổng 3309, root local, `--single-transaction --routines --triggers --default-character-set=utf8mb4`, `--result-file` tới tên file nêu trên. Kết quả thành công, không import/restore/drop/truncate.

Flyway chạy tự động khi test/ứng dụng khởi động. Kiểm tra `flyway_schema_history`: phiên bản 1 và 2 đều `success=1`. Lần restart cuối log: `Successfully validated 2 migrations`, `Schema luxury_hotel is up to date. No migration necessary.`

### 2. Build và kiểm tra trên MariaDB thật

Lệnh chạy từ thư mục dự án:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools\jdk21\jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME=Join-Path (Get-Location) '.tools\maven-home'
$env:DB_USERNAME='root'
$env:DB_PASSWORD=''
$env:TEST_DB_URL='jdbc:mariadb://127.0.0.1:3309/luxury_hotel_test'
$env:HOTEL_IT='true'
.\mvnw.cmd -B -ntp '-Dmaven.repo.local=.tools/m2' verify *> .tools\early-checkout-verify.log
```

Kết quả lúc 20:56:22: **BUILD SUCCESS — Tests run: 17, Failures: 0, Errors: 0, Skipped: 0**. Thời gian Maven 38,055 giây. Tạo `target/luxury-hotel-1.0.0.jar`.

| Bộ kiểm tra | Số test | Kết quả thực tế |
|---|---:|---|
| HotelIntegrationTest | 11 | Thành công, MariaDB thật; HTTP MockMvc, nghiệp vụ và 2 thread đồng thời |
| DemoCheckInIntegrationTest | 1 | Thành công, MariaDB thật; 09:00 đúng ngày nhận/trả ngay, giữ state/room guards |
| StayPolicyTest | 3 | Thành công; demo flag/profile, giờ/ngày, nhận/trả cùng thời điểm |
| MoneyAndStateTest | 2 | Thành công; số tiền và trạng thái |

File chi tiết: `target/surefire-reports/`. `TestDatabaseGuard` kiểm tra tên schema **trước Flyway**; chỉ cho `luxury_hotel_test`. Các test thêm fixture riêng có mã duy nhất; không reset database. Đồng hồ cố định chỉ được inject trong test, không thay giờ ứng dụng đang chạy.

Các tình huống yêu cầu đã thực thi:

| Tình huống | Bằng chứng chạy | Kết quả |
|---|---|---|
| PENDING/CONFIRMED chưa nhận không được checkout | `checkoutRejectsWrongStateMissingActualTimeAndTimeBeforeCheckIn` | Bị từ chối, không hóa đơn |
| Nhận rồi trả ngay, actual bằng nhau | `immediateEarlyCheckoutKeepsBookedPriceAndFreesRemainingDates`, DemoIT | Thành công 14:00 và 09:00 trong ngữ cảnh test |
| Nhiều đêm trả sớm không giảm tiền | IT 3 đêm × 1.234.567,50 | Giữ đủ 3.703.702,50 tiền phòng |
| Dịch vụ chính xác, server tính lại sau preview | IT preview có 2 dịch vụ, ghi thêm 1, POST kèm giá giả | Tổng cuối 4.003.703,25 = phòng + 300.000,75 dịch vụ |
| Hai checkout đồng thời | `concurrentCheckoutCreatesOneInvoiceAndOneTransition` | Một thành công, một lỗi nghiệp vụ; 1 hóa đơn, 1 history checkout |
| Sau checkout không ghi thêm dịch vụ | fullStay + immediateEarlyCheckout | Bị chặn |
| Giải phóng lịch, vẫn phải dọn trước nhận khách mới | immediateEarlyCheckout | Đặt lại cùng ngày được; nhận trước dọn bị chặn, sau dọn được |
| Request checkout cũ khi đã có khách mới | immediateEarlyCheckout | Flash lỗi, phòng khách mới vẫn OCCUPIED |
| Clean/admin READY khi còn khách | `cleaningAndAdminReadyBothRejectAnExistingOccupant` | Cả hai đường ghi bị chặn |
| Failed → retry success | fullStay + immediateEarlyCheckout + Chrome | Cùng hóa đơn, 2 attempts, 1 payment, COMPLETED |
| Hai payment đồng thời | `simultaneousPaymentsCreateExactlyOneReceipt` | Chỉ một khoản thu |
| CUSTOMER gọi preview/checkout | IT HTTP GET và POST | 403 |
| Trả thực tế trước nhận hoặc thiếu giờ nhận | checkoutRejectsWrongState… | Bị chặn, không tự điền giờ |
| Checkout lại đơn đã trả/hoàn tất/hủy | immediateEarlyCheckout + checkoutRejectsWrongState… | Bị chặn; không sửa room/hóa đơn |
| Luồng giờ thông thường/đúng hạn/muộn | fullStay, simultaneousPayments, dateCapacity…, Policy | Nhận 14:00, trả 12:00/12:30 ngày dự kiến thành công |
| Demo trước 14:00 và giới hạn ngoại lệ | DemoIT + Policy | 09:00 đúng ngày được, tương lai/DIRTY/PENDING bị chặn; ngoài demo cờ true không mở ngoại lệ |
| Đặt trùng đồng thời | simultaneousBookings… | Chỉ một đặt trùng thành công, hai khoảng liền kề vẫn được |
| Quyền/sở hữu/CSRF/tài khoản khóa | authenticationAuthorization… + httpCatalog… | Truy cập trái quyền bị chặn, role đăng ký không được nâng |

### 3. Trình duyệt Chrome thật

Chạy `node .tools/early-checkout-browser.cjs`, dùng Playwright Core local `.tools/browser/package`, Chrome cài trên máy. Script đi qua UI từ khách tạo đơn đến lễ tân nhận, thêm dịch vụ và preview. Lần đầu script dừng do dùng nhầm selector `#reason`; template thực tế là `#checkoutReason`. Đã sửa selector, tiếp tục **chính đơn #8**, không tạo thêm đơn thử:

```powershell
$env:HOTEL_BROWSER_RESUME='true'
node .tools\early-checkout-browser.cjs
```

Kết quả JSON `PASS` lúc 21:03:52, `.tools/early-checkout-browser-result.json`. Đây là hai lượt script nối tiếp trên cùng đơn, không tuyên bố lượt đầu đã hoàn thành.

- Đơn #8 `LH-78BD7161-906F-43E1-8350-C0312130A8CB`, phòng **104**, ngày dự kiến **17–20/11/2026**, 3 đêm.
- Nhận thực tế **17/11/2026 21:03:10.764840**, trả thực tế **21:03:49.997544**. Người trả phòng id=2 (lễ tân), lý do được lưu. Không đổi ngày dự kiến.
- Hóa đơn #2 `INV-DD97FB2F-357E-4BA9-9CB8-F18E17418F27`: tiền phòng **3.600.000**, dịch vụ ăn tối **700.000**, tổng **4.300.000 VND**.
- Failed rồi success: cùng mã hóa đơn, một khoản thu, 2 lần thử. Đơn COMPLETED. Dọn phòng qua UI → READY.
- Gửi lại POST checkout với CSRF hợp lệ: redirect chi tiết, flash lỗi; giữ đăng nhập và COMPLETED.
- 18 URL bổ sung được mở thành công, gồm 6 đơn cũ, hóa đơn cũ, hồ sơ, staff dashboard/bookings/invoices/audit, admin users/catalogs/revenue. Không có JavaScript `pageerror`.
- Preview desktop 1440px và mobile 390px; không tràn ngang toàn trang. Bảng dịch vụ cuộn ngang trong khung trên mobile.

Ảnh tại `.tools/early-checkout-desktop.png`, `.tools/early-checkout-mobile.png`, `.tools/early-checkout-paid.png`. Đã xem ảnh mobile để kiểm tra nội dung và bố cục. Artifact trong `.tools` bị gitignore, phục vụ xác minh local.

### 4. Đối chiếu SQL và khởi động lại

Truy vấn đọc tại `.tools/early-checkout-readonly.sql`, thực thi:

```powershell
Get-Content .tools\early-checkout-readonly.sql -Raw |
  & 'C:\xampp\mysql\bin\mysql.exe' --default-character-set=utf8mb4 `
    -h 127.0.0.1 -P 3309 -u root -N -B luxury_hotel |
  Tee-Object -FilePath .tools\early-checkout-sql-result.txt
```

Kết quả: 1 invoice cho booking #8, 1 payment, 2 payment_attempts; room READY; booked nights=3; tổng 4.300.000; dữ liệu 6 đơn cũ/hóa đơn cũ khớp snapshot; fields mới cho đơn cũ NULL. Một câu SELECT seed chạy inline trước đó gặp lỗi quote PowerShell; đã chuyển sang file SQL ở trên và chạy lại thành công.

Đã dừng riêng tiến trình Luxury Hotel cũ và khởi động JAR mới bằng `Start-Process -WindowStyle Hidden`, profile demo, DB_URL 3309, log `.tools/early-checkout-final-app.log`. Lần cuối lúc **21:09:13**: `Started LuxuryHotelApplication`, Tomcat 8080, Flyway schema 2 hiện hành.

```powershell
node .tools\early-checkout-restart.cjs
```

Kết quả **RESTART PASS**: hóa đơn đơn #8 vẫn đã thanh toán 4.300.000đ, phòng 104 READY, các đơn cũ 1..6 HTTP 200. Compare-Object trả không khác biệt. SQL seed `DEMO-TODAY-2026-11-17` vẫn count=1 sau restart. Không reset/dọn database test hay demo.

## Giới hạn xác minh và vấn đề còn lại

- Không còn lỗi chặn luồng chính trong các lần chạy đã nêu. Xem [bảng rà soát](FUNCTIONAL_REVIEW.md) để phân biệt code có sẵn với chức năng đã thực thi.
- Chưa thử tất cả biên nhập liệu, POST hồ sơ riêng, mọi cập nhật danh mục/quyền qua Chrome, mọi trang phân trang, in hệ điều hành, mọi browser hay tải lớn. Không đánh dấu các phần đó là Pass.
- Nhận buổi sáng được kiểm tra bằng test thật MariaDB với Clock inject; lần chạy Chrome là buổi tối. Profile ngoài demo được kiểm tra unit ở StayPolicy, chưa khởi động một bản production hoàn chỉnh.
- Các lỗi URL bộ lọc sai kiểu và một số lỗi quản trị vẫn dùng trang lỗi chung có thông báo; các lỗi action lưu trú chính đã về chi tiết với flash.
- Các dòng CHECKED_IN cũ thiếu giờ nhận thực tế sẽ bị chặn checkout để đối chiếu; migration không bịa thời gian. Sáu đơn thật trên máy hiện tại vẫn mở được.
- Thanh toán mô phỏng, không hoàn tiền/phí trả sớm/ngân hàng thật. Danh mục nhỏ chưa phân trang; PENDING chưa tự hết hạn. Không thực hiện đánh giá vận hành production.
- Log Flyway/MariaDB có thể có cảnh báo dò `WSREP_ON` trên XAMPP; lần này migration, validate và khởi động đều thành công. Không bỏ kiểm tra migration để che lỗi.

Chạy lại cho người dùng: xem [DEMO.md](DEMO.md). Bộ kiểm tra có thể chạy bằng `scripts/check.ps1` với tài khoản DB phù hợp; không dùng schema ứng dụng cho kiểm tra tích hợp.
