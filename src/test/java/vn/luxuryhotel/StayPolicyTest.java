package vn.luxuryhotel;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import vn.luxuryhotel.configuration.*;
import vn.luxuryhotel.entity.Booking;
import vn.luxuryhotel.exception.BusinessException;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class StayPolicyTest {
    private final LocalDate day=LocalDate.of(2031,1,2);
    private final HotelTime time=new HotelTime(Clock.fixed(day.atTime(9,0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),ZoneId.of("Asia/Ho_Chi_Minh")),"14:00","12:00","23:59");
    private Booking booking(){var b=new Booking();b.setCheckIn(day);b.setCheckOut(day.plusDays(2));b.setCheckedInAt(day.atTime(9,0));return b;}
    @Test void flagCannotEnableEarlyCheckInOutsideDemo(){
        var policy=new StayPolicy(time,new MockEnvironment(),true);
        assertThat(policy.isDemoEarlyCheckIn()).isFalse();
        assertThatThrownBy(()->policy.validateCheckIn(booking(),day.atTime(9,0))).isInstanceOf(BusinessException.class);
        assertThatCode(()->policy.validateCheckIn(booking(),day.atTime(14,0))).doesNotThrowAnyException();
    }
    @Test void demoFlagMustBeEnabledAndCannotChangeArrivalDate(){
        var env=new MockEnvironment();env.setActiveProfiles("demo");
        var enabled=new StayPolicy(time,env,true);var disabled=new StayPolicy(time,env,false);
        assertThat(enabled.isDemoEarlyCheckIn()).isTrue();assertThat(disabled.isDemoEarlyCheckIn()).isFalse();
        assertThatCode(()->enabled.validateCheckIn(booking(),day.atTime(0,0))).doesNotThrowAnyException();
        assertThatThrownBy(()->disabled.validateCheckIn(booking(),day.atTime(9,0))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->enabled.validateCheckIn(booking(),day.minusDays(1).atTime(14,0))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->enabled.validateCheckIn(booking(),day.plusDays(1).atTime(9,0))).isInstanceOf(BusinessException.class);
    }
    @Test void earlyCheckoutIsOfficialPolicyOutsideDemoAndAllowsEqualInstants(){
        var policy=new StayPolicy(time,new MockEnvironment(),false);var b=booking();
        assertThatCode(()->policy.validateActualCheckout(b,b.getCheckedInAt())).doesNotThrowAnyException();
        assertThat(policy.isEarlyCheckout(b,day.plusDays(2).atTime(11,59))).isTrue();
        assertThat(policy.isEarlyCheckout(b,day.plusDays(2).atTime(12,0))).isFalse();
        assertThatThrownBy(()->policy.validateActualCheckout(b,day.atTime(8,59))).isInstanceOf(BusinessException.class);
    }
}
