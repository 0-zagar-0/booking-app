package booking.app.dto.payment;

import java.math.BigDecimal;

public record PaymentResponseDto(
        Long id,
        String status,
        Long bookingId,
        BigDecimal amountToPay,
        String currency,
        String paymentMethodType

) {

}
