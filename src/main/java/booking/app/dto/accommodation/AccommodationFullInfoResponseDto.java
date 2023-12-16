package booking.app.dto.accommodation;

import java.math.BigDecimal;
import java.util.Set;

public record AccommodationFullInfoResponseDto(
        Long id,
        String type,
        String address,
        String size,
        Set<String> amenities,
        BigDecimal dailyRate,
        Integer availability
) {
}
