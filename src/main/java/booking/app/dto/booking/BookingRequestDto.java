package booking.app.dto.booking;

import booking.app.validation.DateTime;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingRequestDto {
    @NotNull(message = "Check-in and check-out time is 12:00 a.m")
    @DateTime
    private String checkInDateYearMonthDay;
    @NotNull
    @Min(1)
    private Integer daysOfStay;
    @NotNull
    private Long accommodationId;
}
