package booking.app.service.booking;

import booking.app.dto.booking.BookingRequestDto;
import booking.app.dto.booking.BookingResponseDto;
import booking.app.dto.booking.BookingUpdateRequestDto;
import booking.app.exception.DataProcessingException;
import booking.app.exception.EntityNotFoundException;
import booking.app.mapper.BookingMapper;
import booking.app.model.Booking;
import booking.app.model.User;
import booking.app.model.accommodation.Accommodation;
import booking.app.repository.BookingRepository;
import booking.app.service.accommodation.AccommodationService;
import booking.app.service.user.UserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final DateTimeFormatter PATTERN_OF_DATE =
            DateTimeFormatter.ofPattern("yyyy, MM, dd");

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final UserService userService;
    private final AccommodationService accommodationService;

    @Override
    public BookingResponseDto createBooking(final BookingRequestDto request) {
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                request.getCheckInDateYearMonthDay()
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(request.getDaysOfStay()).minusSeconds(1);
        Accommodation accommodation = checkAndGetAccommodation(request.getAccommodationId());
        checkingDateBookingAndAvailability(checkInDate, checkOutDate, accommodation);

        Booking booking = bookingMapper.toEntity(request, checkInDate, checkOutDate);
        booking.setUser(userService.getAutnenticatedUser());
        booking.setAccommodation(accommodation);

        return bookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    public List<BookingResponseDto> getAllByUserIdAndStatus(
            final Long userId,
            final String status,
            final Pageable pageable
    ) {
        userService.existsById(userId);
        Booking.Status validStatus = checkValidStatus(status);
        return bookingRepository.findByUserIdAndStatus(userId, validStatus, pageable).stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Override
    public List<BookingResponseDto> getAllMyBookings(final Pageable pageable) {
        User user = userService.getAutnenticatedUser();
        return bookingRepository.findAllByUser(user, pageable).stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Override
    public BookingResponseDto getById(final Long id) {
        return bookingMapper.toDto(getBookingById(id));
    }

    @Override
    public void updateById(final Long id, final BookingUpdateRequestDto request) {
        Booking booking = getBookingById(id);
        checkCorrectUserForBooking(booking);

        if (request.getCheckInDateYearMonthDay() != null
                && !request.getCheckInDateYearMonthDay().isEmpty()
                && request.getDaysOfStay() != null) {
            LocalDateTime checkInDate =
                    checkAndParseCheckInDateToLocalDateTime(request.getCheckInDateYearMonthDay());
            LocalDateTime checkOutDate =
                    checkInDate.plusDays(request.getDaysOfStay()).minusSeconds(1);
            checkingDateBookingAndAvailability(
                    checkInDate, checkOutDate, booking.getAccommodation()
            );
            booking.setCheckInDate(checkInDate);
            booking.setCheckOutDate(checkOutDate);
        }

        if (request.getStatus() != null) {
            Booking.Status status = checkValidStatus(request.getStatus());
            booking.setStatus(status);
        }

        bookingRepository.save(booking);
    }

    @Override
    public void deleteById(final Long id) {
        Booking booking = getBookingById(id);
        checkCorrectUserForBooking(booking);
        bookingRepository.deleteById(id);
    }

    private Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find booking by id: " + id)
        );
    }

    private void checkCorrectUserForBooking(Booking booking) {
        User user = userService.getAutnenticatedUser();

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new DataProcessingException("Unable to process booking by id: "
                    + booking.getId() + ", it is not yours");
        }
    }

    private Booking.Status checkValidStatus(String statusName) {
        Booking.Status validStatus = null;

        for (Booking.Status status : Booking.Status.values()) {
            if (statusName.toUpperCase().trim().equals(status.name())) {
                validStatus = status;
            }
        }

        if (validStatus == null) {
            throw new DataProcessingException(
                    "This status does not exist or the data is not entered correctly"
            );
        }

        return validStatus;
    }

    private void checkingDateBookingAndAvailability(
            LocalDateTime checkInDate, LocalDateTime checkOutDate, Accommodation accommodation
    ) {
        List<Booking> allByCheckInDateAndCheckOutDate =
                bookingRepository.findAllBetweenCheckInDateAndCheckOutDate(
                        checkInDate, checkOutDate, accommodation.getId()
                );

        boolean checkAvailability = accommodation.getAvailability()
                - allByCheckInDateAndCheckOutDate.size() <= 0;

        if (!allByCheckInDateAndCheckOutDate.isEmpty() && checkAvailability) {
            throw new DataProcessingException("There are no vacancies in the interval from: "
                    + checkInDate + ", to: " + checkOutDate);
        }
    }

    private LocalDateTime checkAndParseCheckInDateToLocalDateTime(String date) {
        LocalDate localDate = LocalDate.parse(date, PATTERN_OF_DATE);

        if (localDate.isBefore(LocalDate.now())) {
            throw new DataProcessingException("The date is incorrect: " + localDate
                    + " , please enter a date greater than the current one: "
                    + LocalDateTime.now());
        }

        return localDate.atTime(12, 0, 0);
    }

    private Accommodation checkAndGetAccommodation(Long accommodationId) {
        Accommodation accommodation =
                accommodationService.getAccommodationById(accommodationId);

        if (accommodation.getAvailability() == 0) {
            throw new DataProcessingException(
                    "Unable to booking this property, availability is: 0");
        }

        return accommodation;
    }
}
