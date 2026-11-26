package vn.luxuryhotel.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import vn.luxuryhotel.entity.Booking;
import vn.luxuryhotel.exception.BusinessException;
import java.time.LocalDateTime;

@Component
public class StayPolicy {
    private final HotelTime time;
    private final boolean demoEarlyCheckIn;

    public StayPolicy(HotelTime time, Environment environment,
                      @Value("${hotel.demo.allow-early-check-in:false}") boolean allowEarlyCheckIn) {
        this.time = time;
        // Setting the property alone must never enable this exception outside demo.
        this.demoEarlyCheckIn = environment.acceptsProfiles(Profiles.of("demo")) && allowEarlyCheckIn;
    }

    public boolean isDemoEarlyCheckIn() { return demoEarlyCheckIn; }

    public void validateCheckIn(Booking booking, LocalDateTime actual) {
        if (!actual.toLocalDate().equals(booking.getCheckIn())) {
            throw new BusinessException("Chỉ nhận phòng đúng ngày nhận dự kiến. Không nhận đơn ngày tương lai hoặc đã quá ngày nhận.");
        }
        if (actual.isAfter(booking.getCheckIn().atTime(time.noShowTime))) {
            throw new BusinessException("Đã quá giờ nhận phòng " + time.noShowTime + ". Hãy kiểm tra thao tác Không đến.");
        }
        if (!demoEarlyCheckIn && actual.isBefore(booking.getCheckIn().atTime(time.checkInTime))) {
            throw new BusinessException("Chỉ nhận phòng từ " + time.checkInTime + " đúng ngày nhận đã đặt.");
        }
    }

    public void validateActualCheckout(Booking booking, LocalDateTime actual) {
        if (booking.getCheckedInAt() == null) {
            throw new BusinessException("Đơn thiếu thời điểm nhận thực tế. Cần kiểm tra dữ liệu lưu trú trước khi trả phòng; hệ thống không tự suy đoán thời điểm nhận.");
        }
        if (actual.isBefore(booking.getCheckedInAt())) {
            throw new BusinessException("Thời điểm trả thực tế không được trước thời điểm nhận thực tế.");
        }
    }

    public boolean isEarlyCheckout(Booking booking, LocalDateTime actual) {
        return actual.isBefore(booking.getCheckOut().atTime(time.checkOutTime));
    }
}
