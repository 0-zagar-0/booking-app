package booking.app.dto.booking;

import booking.app.validation.DateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingUpdateRequestDto {
    @DateTime
    private String checkInDateYearMonthDay;
    private Integer daysOfStay;
    private String status;
}
