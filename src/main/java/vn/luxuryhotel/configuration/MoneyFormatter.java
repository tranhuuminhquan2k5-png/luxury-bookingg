package vn.luxuryhotel.configuration;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
@Component("money")
public class MoneyFormatter {
    public String format(BigDecimal value){
        if(value==null)return "0";
        return new DecimalFormat("#,##0.##",DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"))).format(value);
    }
}
