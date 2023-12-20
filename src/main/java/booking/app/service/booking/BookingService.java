package booking.app.service.booking;

import booking.app.dto.booking.BookingRequestDto;
import booking.app.dto.booking.BookingResponseDto;
import booking.app.dto.booking.BookingUpdateRequestDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto request);

    List<BookingResponseDto> getAllByUserIdAndStatus(Long userId, String status, Pageable pageable);

    List<BookingResponseDto> getAllMyBookings(Pageable pageable);

    BookingResponseDto getById(Long id);

    void updateById(Long id, BookingUpdateRequestDto request);

    void deleteById(Long id);
}
