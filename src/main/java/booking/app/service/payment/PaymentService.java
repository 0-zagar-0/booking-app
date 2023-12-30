package booking.app.service.payment;

import booking.app.dto.payment.PaymentCanceledResponseDto;
import booking.app.dto.payment.PaymentCreateRequestDto;
import booking.app.dto.payment.PaymentResponseDto;
import booking.app.dto.payment.PaymentSuccessResponseDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    List<PaymentResponseDto> getAllPaymentByUserId(Long userId, Pageable pageable);

    String initializeSession(PaymentCreateRequestDto request);

    PaymentSuccessResponseDto confirmPaymentIntent();

    PaymentCanceledResponseDto paymentCancellation();
}
