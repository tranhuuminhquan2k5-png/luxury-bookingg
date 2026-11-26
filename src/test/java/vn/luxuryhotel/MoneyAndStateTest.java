package vn.luxuryhotel;
import org.junit.jupiter.api.Test;
import vn.luxuryhotel.entity.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class MoneyAndStateTest {
    @Test void moneyUsesExactDecimalArithmetic(){var b=new Booking();b.setNights(3);b.setNightlyPrice(new BigDecimal("1234567.25"));assertThat(b.getRoomTotal()).isEqualByComparingTo("3703701.75");var s=new BookingServiceItem();s.setQuantity(7);s.setUnitPrice(new BigDecimal("100.15"));assertThat(s.getTotal()).isEqualByComparingTo("701.05");}
    @Test void futureAvailabilityDiffersFromPhysicalReadiness(){assertThat(RoomStatus.DIRTY.bookable()).isTrue();assertThat(RoomStatus.OCCUPIED.bookable()).isTrue();assertThat(RoomStatus.MAINTENANCE.bookable()).isFalse();assertThat(RoomStatus.INACTIVE.bookable()).isFalse();assertThat(BookingStatus.PENDING.holdsRoom()).isTrue();assertThat(BookingStatus.AWAITING_PAYMENT.holdsRoom()).isFalse();}
}
