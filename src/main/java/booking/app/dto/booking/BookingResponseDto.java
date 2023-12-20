package booking.app.dto.booking;

import java.time.LocalDateTime;

public record BookingResponseDto(
        Long id,
        LocalDateTime checkInDate,
        LocalDateTime checkOutDate,
        Long accommodationId,
        Long userId,
        String status
) {
}
