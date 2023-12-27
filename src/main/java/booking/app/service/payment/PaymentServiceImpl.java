package booking.app.service.payment;

import booking.app.dto.accommodation.AccommodationFullInfoResponseDto;
import booking.app.dto.booking.BookingResponseDto;
import booking.app.dto.booking.BookingUpdateRequestDto;
import booking.app.dto.payment.PaymentCreateRequestDto;
import booking.app.exception.CardProcessingException;
import booking.app.exception.DataProcessingException;
import booking.app.exception.EntityNotFoundException;
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
import com.stripe.model.PaymentMethod;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.ChargeListParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final String BOOKING_PENDING_STATUS = "PENDING";
    private static final String BOOKING_CONFIRMED_STATUS = "CONFIRMED";
    private static final String BOOKING_CANCELED_STATUS = "CANCELED";
    private static final String BOOKING_ID_METADATA = ".Booking_id";
    private static final String PAYMENT_PAID_STATUS = "PAID";
    private static final String BOOKING_CLASS_NAME = "BOOKING";
    private static final String PAYMENT_CLASS_NAME = "PAYMENT";
    private static final String CURRENCY = "USD";

    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final BookingService bookingService;
    private final AccommodationService accommodationService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    private Session session;

    @Transactional
    @Override
    public void initializeSession(PaymentCreateRequestDto request) {
        final User user = userService.getAutnenticatedUser();
        Long totalAmount = checkValidBookingIdsAndGetTotalAmount(request.getBookingIds(), user);
        createSession(
                request.getProductName(),
                totalAmount,
                request.getBookingIds(),
                user,
                request.getPaymentCardToken()
        );

        for (Long bookingId : request.getBookingIds()) {
            paymentRepository.save(createPayment(bookingId));
        }
    }

    @Transactional
    @Override
    public void confirmPaymentIntent() {
        PaymentIntent intent = null;

        try {
            intent = PaymentIntent.retrieve(
                    session.getPaymentIntent(), createRequestOption()
            );
            PaymentIntent confirmedIntent = intent.confirm(createRequestOption());

            if ("succeeded".equals(confirmedIntent.getStatus())) {
                ChargeCollection charges = getChargeCollection(confirmedIntent);

                if (!charges.getData().isEmpty()) {
                    session.setSuccessUrl(charges.getData().get(0).getReceiptUrl());
                    updateStatus(BOOKING_CLASS_NAME, confirmedIntent, BOOKING_CONFIRMED_STATUS);
                    updateStatus(PAYMENT_CLASS_NAME, confirmedIntent, PAYMENT_PAID_STATUS);
                }
            }
        } catch (CardException cardException) {
            updatePaymentStatus(Payment.Status.FAILED);
            ChargeCollection charges = getChargeCollection(intent);

            throw new CardProcessingException(
                    charges.getData().get(0).getFailureMessage()
            );
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void paymentCancellation() {
        try {
            PaymentIntent intent =
                    PaymentIntent.retrieve(session.getPaymentIntent(), createRequestOption());
            intent.cancel(createRequestOption());

            updatePaymentStatus(Payment.Status.CANCELED);

            BookingUpdateRequestDto requestUpdate = new BookingUpdateRequestDto();
            requestUpdate.setStatus(BOOKING_CANCELED_STATUS);
            bookingService.updateById(getPaymentBySessionId().getBookingId(), requestUpdate);
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }

    private void createSession(
            String productName,
            Long amountPrice,
            List<Long> bookingsIds,
            User user,
            String cardToken
    ) {
        final Product product = createProduct(productName);
        final Price price = createPrice(amountPrice, product.getId());
        final Customer customer = createCustomer(user);
        final PaymentIntent paymentIntent =
                createPaymentIntent(productName, price, bookingsIds, customer, cardToken);

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
            List<Long> bookingIds,
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

        addBookingIdsToIntentMetadata(params, bookingIds);
        try {
            return PaymentIntent.create(params, createRequestOption());
        } catch (StripeException e) {
            throw new RuntimeException("Can't create payment intent stripe error: "
                    + e.getMessage()
            );
        }
    }

    private PaymentMethod createPaymentMethod(String token) {
        String tokenCard = token;
        PaymentMethodCreateParams params = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.CARD)
                .setCard(PaymentMethodCreateParams.Token.builder()
                        .setToken(tokenCard)
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

    private Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingIdAndSessionId(bookingId, session.getId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Can't find payment by booking id: "
                                + bookingId)
                );
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

    private void updateStatus(String updateClassName, PaymentIntent intent, String status) {
        final ChargeCollection charges = getChargeCollection(intent);
        final Map<String, String> metadata = charges.getData().get(0).getMetadata();

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (entry.getKey().contains(BOOKING_ID_METADATA)) {
                if (updateClassName.equals(BOOKING_CLASS_NAME)) {
                    BookingUpdateRequestDto updateReq = new BookingUpdateRequestDto();
                    updateReq.setStatus(status);
                    bookingService.updateById(Long.parseLong(entry.getValue()), updateReq);
                } else if (updateClassName.equals(PAYMENT_CLASS_NAME)) {
                    Payment payment = getPaymentByBookingId(Long.parseLong(entry.getValue()));
                    payment.setStatus(Payment.Status.valueOf(status));
                    paymentRepository.save(payment);
                }
            }
        }
    }

    private void updatePaymentStatus(Payment.Status status) {
        final Payment payment = getPaymentBySessionId();
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    private Long checkValidBookingIdsAndGetTotalAmount(List<Long> bookingIds, User user) {
        Pageable pageable = PageRequest.of(0, 100);
        final List<BookingResponseDto> pendingUserBookings = bookingService
                .getAllByUserIdAndStatus(user.getId(), BOOKING_PENDING_STATUS, pageable);
        AtomicLong amount = new AtomicLong(0L);

        for (Long bookingId : bookingIds) {
            BookingResponseDto bookingById = bookingService.getById(bookingId);

            if (!pendingUserBookings.contains(bookingService.getById(bookingId))) {
                throw new DataProcessingException("The booking for this ID: " + bookingId
                        + " is not on your list or has already been paid for"
                );
            }

            AccommodationFullInfoResponseDto accommodationById = accommodationService
                    .getById(bookingService.getById(bookingId).accommodationId());
            long days = ChronoUnit.DAYS.between(
                    bookingById.checkInDate(), bookingById.checkOutDate().plusSeconds(1)
            );
            amount.addAndGet(accommodationById.dailyRate().longValue() * days);
        }
        return amount.get();
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

    private void addBookingIdsToIntentMetadata(
            PaymentIntentCreateParams params, List<Long> bookingIds
    ) {
        for (int i = 0; i < bookingIds.size(); i++) {
            params.getMetadata().put((i + 1) + BOOKING_ID_METADATA, bookingIds.get(i).toString());
        }
    }
}
