package vn.luxuryhotel.configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import java.time.*;
@Component
public class HotelTime {
    private final Clock clock;
    public final LocalTime checkInTime,checkOutTime,noShowTime;
    public HotelTime(Clock clock,@Value("${hotel.check-in-time}") String in,
                     @Value("${hotel.check-out-time}") String out,@Value("${hotel.no-show-time}") String noShow) {
        this.clock=clock; checkInTime=LocalTime.parse(in); checkOutTime=LocalTime.parse(out); noShowTime=LocalTime.parse(noShow);
    }
    public LocalDate today(){ return LocalDate.now(clock); }
    public LocalDateTime now(){ return LocalDateTime.now(clock); }
    @Configuration public static class ClockConfig {
        @Bean public Clock hotelClock(@Value("${hotel.zone}") String zone){ return Clock.system(ZoneId.of(zone)); }
    }
}
