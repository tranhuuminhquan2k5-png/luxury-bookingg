package vn.luxuryhotel.dto;

import vn.luxuryhotel.entity.Booking;
import vn.luxuryhotel.entity.BookingServiceItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only preview. POST never accepts these amounts and recalculates under lock. */
public record CheckoutSummary(Booking booking, List<BookingServiceItem> services,
                              LocalDateTime actualCheckout, boolean early,
                              BigDecimal serviceTotal, BigDecimal total) {}
