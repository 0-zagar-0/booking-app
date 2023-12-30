package booking.app.controller;

import booking.app.dto.payment.PaymentCanceledResponseDto;
import booking.app.dto.payment.PaymentCreateRequestDto;
import booking.app.dto.payment.PaymentResponseDto;
import booking.app.dto.payment.PaymentSuccessResponseDto;
import booking.app.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment management", description = "Endpoints for payments action")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "Get all user payments", description = "Get user payments")
    @ResponseStatus(HttpStatus.OK)
    public List<PaymentResponseDto> getPaymentByUser(
            @RequestParam(name = "user_id") Long userId, Pageable pageable
    ) {
        return paymentService.getAllPaymentByUserId(userId, pageable);
    }

    @PostMapping
    @Operation(summary = "Initialize session",
            description = "Initialize session and create payment")
    @ResponseStatus(HttpStatus.CREATED)
    public String initializeSession(@RequestBody PaymentCreateRequestDto request) {
        return paymentService.initializeSession(request);
    }

    @GetMapping("/success")
    @Operation(summary = "Success payment", description = "Confirm payment intent")
    @ResponseStatus(HttpStatus.OK)
    public PaymentSuccessResponseDto successfulPaymentProcessing() {
        return paymentService.confirmPaymentIntent();
    }

    @GetMapping("/cancel")
    @Operation(summary = "Payment cancellation", description = "Payment cancellation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public PaymentCanceledResponseDto paymentCancellation() {
        return paymentService.paymentCancellation();
    }

}
