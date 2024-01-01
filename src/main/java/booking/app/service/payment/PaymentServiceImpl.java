package booking.app.service.payment;

import booking.app.dto.accommodation.AccommodationFullInfoResponseDto;
import booking.app.dto.booking.BookingResponseDto;
import booking.app.dto.booking.BookingUpdateRequestDto;
import booking.app.dto.payment.PaymentCanceledResponseDto;
import booking.app.dto.payment.PaymentCreateRequestDto;
import booking.app.dto.payment.PaymentResponseDto;
import booking.app.dto.payment.PaymentSuccessResponseDto;
import booking.app.exception.CardProcessingException;
import booking.app.exception.DataProcessingException;
import booking.app.exception.EntityNotFoundException;
import booking.app.mapper.PaymentMapper;
import booking.app.model.Booking;
import booking.app.model.Payment;
import booking.app.model.User;
import booking.app.repository.PaymentRepository;
import booking.app.service.accommodation.AccommodationService;
import booking.app.service.booking.BookingService;
import booking.app.service.user.UserService;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.ChargeCollection;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentIntentCollection;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.ChargeListParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentListParams;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final String BOOKING_ID_METADATA = "Booking_id";
    private static final String PAYMENT_ID_METADATA = "Payment_id";
    private static final String CURRENCY = "USD";
    private static final String SUCCESS_STATUS = "succeeded";

    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final BookingService bookingService;
    private final AccommodationService accommodationService;
    private final PaymentMapper paymentMapper;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    private Session session;

    @Override
    public List<PaymentResponseDto> getAllPaymentByUserId(final Long userId, Pageable pageable) {
        User user = userService.getById(userId);
        List<PaymentResponseDto> payments = new ArrayList<>();

        try {
            CustomerListParams customerParams = CustomerListParams.builder()
                    .setEmail(user.getEmail())
                    .build();
            CustomerCollection customers = Customer.list(customerParams, createRequestOption());
            Customer customer = customers.getData().get(0);

            PaymentIntentListParams paymentIntentParams = PaymentIntentListParams.builder()
                    .setCustomer(customer.getId())
                    .build();
            PaymentIntentCollection paymentIntents =
                    PaymentIntent.list(paymentIntentParams, createRequestOption());

            for (PaymentIntent intent : paymentIntents.getData()) {
                Long paymentId = Long.parseLong(intent.getMetadata().get(PAYMENT_ID_METADATA));
                Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                        () -> new EntityNotFoundException("Can't find payment by id: " + paymentId)
                );
                payments.add(paymentMapper.toDto(
                        payment, intent.getPaymentMethodTypes().get(0), intent.getCurrency())
                );
            }
        } catch (StripeException e) {
            throw new RuntimeException("Stripe exception: " + e.getMessage());
        }
        return payments;
    }

    @Transactional
    @Override
    public String initializeSession(PaymentCreateRequestDto request) {
        final User user = userService.getAutnenticatedUser();
        Long totalAmount = checkValidBookingIdsAndGetTotalAmount(request.getBookingId(), user);
        createSession(
                request.getProductName(),
                totalAmount,
                request.getBookingId(),
                user,
                request.getPaymentCardToken()
        );
        paymentRepository.save(createPayment(request.getBookingId()));
        addToPaymentIntentMetadataPaymentId();
        return session.getUrl();
    }

    @Override
    public PaymentSuccessResponseDto confirmPaymentIntent() {
        PaymentIntent intent = null;

        try {
            intent = PaymentIntent.retrieve(
                    session.getPaymentIntent(), createRequestOption()
            );
            PaymentIntent confirmedIntent = intent.confirm(createRequestOption());

            if (SUCCESS_STATUS.equals(confirmedIntent.getStatus())) {
                Long bookingId = Long.parseLong(
                        confirmedIntent.getMetadata().get(BOOKING_ID_METADATA)
                );
                updateBookingStatus(bookingId, Booking.Status.CONFIRMED.name());
                updatePaymentStatus(Payment.Status.PAID);
                updateSessionSuccessUrl(confirmedIntent);
            }
        } catch (CardException cardException) {
            updatePaymentStatus(Payment.Status.FAILED);
            updateBookingStatus(
                    getPaymentBySessionId().getBookingId(), Booking.Status.FAILED.name()
            );
            ChargeCollection charges = getChargeCollection(intent);
            throw new CardProcessingException(
                    charges.getData().get(0).getFailureMessage()
            );
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
        return new PaymentSuccessResponseDto(session.getSuccessUrl());
    }

    @Transactional
    @Override
    public PaymentCanceledResponseDto paymentCancellation() {
        try {
            PaymentIntent intent =
                    PaymentIntent.retrieve(session.getPaymentIntent(), createRequestOption());
            intent.cancel(createRequestOption());
            updatePaymentStatus(Payment.Status.CANCELED);
            updateBookingStatus(
                    getPaymentBySessionId().getBookingId(), Booking.Status.CANCELED.name()
            );
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
        return new PaymentCanceledResponseDto(session.getCancelUrl().toString());
    }

    private void createSession(
            String productName,
            Long amountPrice,
            Long bookingId,
            User user,
            String cardToken
    ) {
        final Product product = createProduct(productName);
        final Price price = createPrice(amountPrice, product.getId());
        final Customer customer = createCustomer(user);
        final PaymentIntent paymentIntent =
                createPaymentIntent(productName, price, bookingId, customer, cardToken);

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://yourwebsite.com/success")
                .setCancelUrl("https://yourwebsite.com/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(price.getId())
                        .setQuantity(1L)
                        .build())
                .setCustomer(customer.getId())
                .build();
        try {
            session = Session.create(params, createRequestOption());
            session.setPaymentIntent(paymentIntent.getId());
        } catch (StripeException e) {
            throw new RuntimeException("Can't create session stripe error: " + e.getMessage());
        }
    }

    private Payment createPayment(Long bookingId) {
        Payment payment = new Payment();
        payment.setStatus(Payment.Status.PENDING);
        payment.setBookingId(bookingId);
        payment.setSessionId(session.getId());

        try {
            URL url = new URL(session.getUrl());
            payment.setSessionUrl(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL error: " + e.getMessage());
        }

        payment.setAmountToPay(BigDecimal.valueOf(getAmountForBooking(bookingId)));
        return payment;
    }

    private PaymentIntent createPaymentIntent(
            String productName,
            Price price,
            Long bookingId,
            Customer customer,
            String cardToken
    ) {
        PaymentMethod paymentMethod = createPaymentMethod(cardToken);
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(price.getUnitAmount())
                .setCurrency(CURRENCY)
                .setPaymentMethod(paymentMethod.getId())
                .putMetadata("Product_name", productName)
                .setCustomer(customer.getId())
                .build();

        addBookingIdToIntentMetadata(params, bookingId);
        try {
            return PaymentIntent.create(params, createRequestOption());
        } catch (StripeException e) {
            throw new RuntimeException("Can't create payment intent stripe error: "
                    + e.getMessage()
            );
        }
    }

    private PaymentMethod createPaymentMethod(String token) {
        PaymentMethodCreateParams params = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.CARD)
                .setCard(PaymentMethodCreateParams.Token.builder()
                        .setToken(token)
                        .build()
                )
                .build();
        try {
            return PaymentMethod.create(params, createRequestOption());
        } catch (StripeException e) {
            throw new RuntimeException("Can't create payment method, stripe error: "
                    + e.getMessage()
            );
        }
    }

    private Customer createCustomer(User user) {
        final Customer customer = checkExistsCustomer(user.getEmail());
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setName(user.getFirstName() + " " + user.getLastName())
                .setEmail(user.getEmail())
                .build();
        try {
            if (customer != null) {
                return customer;
            }

            return Customer.create(params, createRequestOption());
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }

    private Price createPrice(Long amount, String productId) {
        try {
            PriceCreateParams params = PriceCreateParams.builder()
                    .setUnitAmount(amount * 100)
                    .setCurrency("USD")
                    .setProduct(productId)
                    .build();

            return Price.create(params, createRequestOption());
        } catch (StripeException e) {
            throw new RuntimeException("Can't create price: " + e.getMessage());
        }
    }

    private Product createProduct(String productName) {
        try {
            ProductCreateParams params = ProductCreateParams.builder()
                    .setName(productName)
                    .build();
            return Product.create(params, createRequestOption());
        } catch (StripeException e) {
            throw new RuntimeException("Can't create product: " + e.getMessage());
        }
    }

    private RequestOptions createRequestOption() {
        return RequestOptions.builder()
                .setApiKey(stripeSecretKey)
                .build();
    }

    private Payment getPaymentBySessionId() {
        return paymentRepository.findBySessionId(session.getId()).orElseThrow(
                () -> new EntityNotFoundException("Can't find payment by session id: "
                        + session.getId())
        );
    }

    private Long getAmountForBooking(Long bookingId) {
        final BookingResponseDto bookingResponseDto = bookingService.getById(bookingId);
        final AccommodationFullInfoResponseDto accommodationDto =
                accommodationService.getById(bookingResponseDto.accommodationId());
        Long days = ChronoUnit.DAYS.between(
                bookingResponseDto.checkInDate(), bookingResponseDto.checkOutDate().plusSeconds(1)
        );
        return accommodationDto.dailyRate().longValue() * days;
    }

    private ChargeCollection getChargeCollection(PaymentIntent intent) {
        ChargeListParams chargeListParams = ChargeListParams.builder()
                .setPaymentIntent(intent.getId())
                .build();
        try {
            return Charge.list(chargeListParams, createRequestOption());
        } catch (StripeException e) {
            throw new RuntimeException("Can't get charge collection: " + e.getMessage());
        }
    }

    private void updateBookingStatus(Long bookingId, String status) {
        BookingUpdateRequestDto updateReq = new BookingUpdateRequestDto();
        updateReq.setStatus(status);
        bookingService.updateById(bookingId, updateReq);
    }

    private void updatePaymentStatus(Payment.Status status) {
        Payment payment = getPaymentBySessionId();
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    private void updateSessionSuccessUrl(PaymentIntent intent) {
        ChargeCollection charges = getChargeCollection(intent);

        if (!charges.getData().isEmpty()) {
            session.setSuccessUrl(charges.getData().get(0).getReceiptUrl());
        }
    }

    private Long checkValidBookingIdsAndGetTotalAmount(Long bookingId, User user) {
        BookingResponseDto bookingById = bookingService.getById(bookingId);
        checkBookingAndGenerateExceptionMessage(bookingById, user.getId());

        AccommodationFullInfoResponseDto accommodationById = accommodationService
                .getById(bookingById.accommodationId());
        long days = ChronoUnit.DAYS.between(
                bookingById.checkInDate(), bookingById.checkOutDate().plusSeconds(1)
        );

        AtomicLong amount = new AtomicLong(0L);
        amount.addAndGet(accommodationById.dailyRate().longValue() * days);
        return amount.get();
    }

    private void checkBookingAndGenerateExceptionMessage(
            BookingResponseDto booking, Long userId
    ) {
        if (!booking.userId().equals(userId)) {
            throw new DataProcessingException("Booking for this ID: " + booking.id()
                    + " not on your bookings.");
        }

        if (booking.status().equals(Booking.Status.CONFIRMED.name())) {
            throw new DataProcessingException("This booking has already been paid, "
                    + "please check your reservations");
        }

        if (booking.status().equals(Booking.Status.CANCELED.name())) {
            throw new DataProcessingException("This booking has been cancelled. "
                    + "Please create a new booking and try the payment again.");
        }

        if (booking.status().equals(Booking.Status.FAILED.name())) {
            throw new DataProcessingException("This booking has been failed. "
                    + "Please create a new booking and try the payment again.");
        }
    }

    private Customer checkExistsCustomer(String email) {
        CustomerListParams params = CustomerListParams.builder()
                .setEmail(email)
                .build();
        try {
            CustomerCollection customers = Customer.list(params, createRequestOption());

            if (!customers.getData().isEmpty()) {
                return customers.getData().get(0);
            }

            return null;
        } catch (StripeException e) {
            throw new RuntimeException("Stripe exception: " + e.getMessage());
        }
    }

    private void addToPaymentIntentMetadataPaymentId() {
        try {
            PaymentIntentUpdateParams params = PaymentIntentUpdateParams.builder()
                    .putMetadata(PAYMENT_ID_METADATA, getPaymentBySessionId().getId().toString())
                    .build();
            PaymentIntent intent = PaymentIntent.retrieve(
                    session.getPaymentIntent(), createRequestOption()
            );
            intent.update(params, createRequestOption());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    private void addBookingIdToIntentMetadata(
            PaymentIntentCreateParams params, Long bookingId
    ) {
        params.getMetadata().put(BOOKING_ID_METADATA, bookingId.toString());
    }
}
