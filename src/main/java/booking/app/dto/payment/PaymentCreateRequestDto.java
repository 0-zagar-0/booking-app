package booking.app.dto.payment;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCreateRequestDto {
    private String productName;
    private List<Long> bookingIds;
    @Builder.Default
    private String paymentCardToken = "tok_visa";
}
