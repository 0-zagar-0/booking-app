package booking.app.dto.accommodation;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccommodationUpdateRequestDto {
    private Set<String> amenities;
    @Positive
    private BigDecimal dailyRate;
    @Positive
    private Integer availability;
}
