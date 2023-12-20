package booking.app.repository;

import booking.app.model.Booking;
import booking.app.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b "
            + "FROM Booking b "
            + "WHERE (b.checkInDate BETWEEN :checkInDate AND :checkOutDate "
            + "OR b.checkOutDate BETWEEN :checkInDate AND :checkOutDate) "
            + "AND b.accommodation.id = :accommodationId")
    @EntityGraph(attributePaths = {"accommodation", "user"})
    List<Booking> findAllBetweenCheckInDateAndCheckOutDate(
            LocalDateTime checkInDate, LocalDateTime checkOutDate, Long accommodationId
    );

    @EntityGraph(attributePaths = {"accommodation", "user"})
    Page<Booking> findByUserIdAndStatus(Long id, Booking.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"accommodation", "user"})
    Page<Booking> findAllByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"accommodation", "user"})
    Optional<Booking> findById(Long id);
}
