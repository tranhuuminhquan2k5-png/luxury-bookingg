# Demo liên tục Luxury Hotel

Bản cập nhật 17/11/2026. Không cần chờ đến ngày trả dự kiến; tiền phòng vẫn tính toàn bộ số đêm đã đặt.

## Mở dự án

Trong Git Bash:

```bash
cd '/f/New folder/kiemthuphanmen'
unset DB_URL
./run-demo.cmd
```

Trong PowerShell:

```powershell
Set-Location 'F:\New folder\kiemthuphanmen'
Remove-Item Env:DB_URL -ErrorAction SilentlyContinue
.\run-demo.cmd
```

Đợi `Started LuxuryHotelApplication`, mở **http://localhost:8080** và giữ terminal chạy. Chỉ mở một bản ứng dụng trên 8080. Launcher tự kiểm tra/khởi động MariaDB XAMPP 3309 trên máy này; không cần Apache. Không chọn MySQL 3306 hoặc MariaDB 3308 của dự án khác. Trong Git Bash dùng `./`, không dùng `.\`.

Nếu website đã chạy từ phiên trước, dùng ngay bản đang chạy. Muốn chạy lại sau sửa code, Ctrl+C trong terminal cũ rồi chạy launcher. Nếu báo cổng đã dùng nhưng không tìm thấy terminal cũ, dùng `powershell -ExecutionPolicy Bypass -File ./scripts/start.ps1 -Demo -Port 8081` với `DB_USERNAME=root`, rồi mở localhost:8081; không dừng tiến trình lạ.

## Đi qua toàn bộ quy trình

Các tài khoản demo dùng chung mật khẩu `LuxuryDemo!2026`:

| Vai trò | Email |
|---|---|
| Khách | `customer@luxury.local` |
| Lễ tân | `staff@luxury.local` |
| Quản trị | `admin@luxury.local` |

1. Đăng nhập **khách**. Tại trang chủ, chọn ngày nhận **hôm nay**, ngày trả sau **3 ngày**, 2 khách. Tìm phòng → Xem chi tiết → Tiếp tục đặt phòng → Gửi yêu cầu. Ghi lại mã đơn và số phòng.
2. Đăng xuất, đăng nhập **lễ tân**. Mở **Đặt phòng**, tìm mã đơn vừa tạo. Bấm **Xác nhận yêu cầu**.
3. Bấm **Nhận phòng**. Phải đúng ngày nhận, đơn CONFIRMED và phòng Sẵn sàng. Nếu phòng Chờ dọn, vào **Tình trạng phòng** và xác nhận đã dọn trước.
4. Chọn dịch vụ và số lượng, bấm **Ghi nhận**. Giá được lưu tại thời điểm ghi nhận.
5. Bấm **Trả phòng** ngay, không cần chờ ngày mai. Trang xác nhận hiển thị ngày dự kiến, thời điểm thực tế, **3 đêm tính tiền**, tiền phòng, dịch vụ, tổng tiền và cảnh báo trả sớm. Có thể nhập lý do, rồi **Xác nhận trả phòng & lập hóa đơn**.
6. Mở **Xem hóa đơn**. Thử **Mô phỏng thất bại**: hóa đơn vẫn Chưa thanh toán, đơn Chờ thanh toán. Sau đó **Mô phỏng thành công**: hóa đơn Đã thanh toán, đơn Hoàn tất, một khoản thu. Không tạo hóa đơn mới khi thử lại.
7. Vào **Tình trạng phòng**, tìm đúng số phòng, bấm **Xác nhận đã dọn**. Phòng trở về Sẵn sàng. Lịch còn lại đã được giải phóng từ lúc checkout; khách tiếp theo chỉ được nhận sau khi dọn.

Ví dụ thực tế đã chạy: phòng 104, 17–20/11/2026, 3 × 1.200.000đ + 2 suất ăn tối × 350.000đ = **4.300.000đ**, nhận và trả cùng ngày. Đơn #8 đã Hoàn tất và giữ lại để tra cứu; để demo lại, tạo đơn mới. Không đưa đơn đã hoàn tất về trạng thái cũ bằng SQL.

## Demo vào buổi sáng

Profile demo mặc định bật `HOTEL_DEMO_EARLY_CHECK_IN=true`. Khi thấy banner **Chế độ demo**, lễ tân được nhận trước 14:00 **chỉ đúng ngày nhận dự kiến**. Vẫn không nhận đơn ngày mai, đơn chưa xác nhận, phòng chưa dọn hoặc phòng đang có khách. Mốc kết thúc nhận 23:59 vẫn giữ nguyên.

Muốn kiểm tra giờ thông thường, đặt cờ false rồi khởi động lại:

```bash
export HOTEL_DEMO_EARLY_CHECK_IN=false
./run-demo.cmd
```

Bật lại bằng `export HOTEL_DEMO_EARLY_CHECK_IN=true`. Ngoài profile demo, cờ không mở ngoại lệ này. **Trả sớm vẫn hoạt động ngoài demo**; không liên quan cờ nhận trước 14:00.

Seed có thể thêm một đơn `DEMO-TODAY-yyyy-MM-dd` nhận hôm nay, 2 đêm, trạng thái Chờ xác nhận khi có phòng READY phù hợp. Khởi động lại trong ngày không tạo bản sao, không sửa ngày hay trạng thái các đơn cũ. Không đổi đồng hồ máy tính/database để demo.

## Những thông báo cần hiểu

- **Thao tác không hợp lệ với trạng thái…**: đơn chưa nhận hoặc đã kết thúc; tải lại chi tiết. Gửi lại checkout không lập hóa đơn thứ hai.
- **Phòng phải Sẵn sàng…**: xử lý dọn phòng trước khi nhận khách; không đổi trạng thái bằng SQL.
- **Chỉ nhận đúng ngày…**: tạo đơn mới có ngày nhận hôm nay; các đơn demo cố định cũ có thể đã quá hạn.
- **Thiếu thời điểm nhận thực tế**: dữ liệu cũ cần được đối chiếu, hệ thống không tự bịa thời gian để trả phòng.
- Thanh toán chỉ mô phỏng. Trả sớm không giảm tiền theo vài phút thực ở, không hoàn tiền và không tính phí trả sớm.

Xem [README](../README.md), [bảng rà soát](FUNCTIONAL_REVIEW.md) và [ghi nhận xác minh kỹ thuật](EARLY_CHECKOUT_VERIFICATION.md).
