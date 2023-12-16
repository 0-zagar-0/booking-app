package booking.app.dto.accommodation;

import booking.app.validation.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccommodationRequestDto {
    @NotBlank
    private String type;
    @NotBlank
    @Address
    private String address;
    @NotBlank
    private String size;
    @NotEmpty
    private Set<String> amenities;
    @NotNull
    @Positive
    private BigDecimal dailyRate;
    @NotNull
    @Positive
    private Integer availability;
}
