package booking.app.repository;

import booking.app.model.accommodation.Address;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
    boolean existsByAddress(String address);

    Optional<Address> findByAddress(String address);

}
