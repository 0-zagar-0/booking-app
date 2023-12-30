package booking.app.repository;

import booking.app.model.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingIdAndSessionId(Long bookingId, String sessionId);

    Optional<Payment> findBySessionId(String sessionId);
}
