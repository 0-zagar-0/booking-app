package booking.app.service.payment;

import booking.app.dto.payment.PaymentCreateRequestDto;

public interface PaymentService {
    void initializeSession(PaymentCreateRequestDto request);

    void confirmPaymentIntent();

    void paymentCancellation();
}
