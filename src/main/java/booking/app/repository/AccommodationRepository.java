package booking.app.repository;

import booking.app.model.accommodation.Accommodation;
import booking.app.model.accommodation.Address;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {
    Optional<Accommodation> findByTypeAndAddressIdAndSizeAndDailyRate(
            Accommodation.Type type,
            Long addressId,
            Accommodation.Size gsize,
            BigDecimal dailyRate);

    @EntityGraph(attributePaths = {"address", "amenities"})
    Page<Accommodation> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"address", "amenities"})
    Optional<Accommodation> findById(Long id);

    @EntityGraph(attributePaths = {"address", "amenities"})
    List<Accommodation> findByAddress(Address address);
}
