package booking.app.service.booking;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import booking.app.dto.booking.BookingRequestDto;
import booking.app.dto.booking.BookingResponseDto;
import booking.app.dto.booking.BookingUpdateRequestDto;
import booking.app.exception.DataProcessingException;
import booking.app.exception.EntityNotFoundException;
import booking.app.mapper.BookingMapper;
import booking.app.model.Booking;
import booking.app.model.User;
import booking.app.model.accommodation.Accommodation;
import booking.app.model.accommodation.Address;
import booking.app.model.accommodation.Amenity;
import booking.app.repository.BookingRepository;
import booking.app.service.accommodation.AccommodationService;
import booking.app.service.user.UserService;
import booking.app.telegram.BookingBot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    private static final DateTimeFormatter PATTERN_OF_DATE =
            DateTimeFormatter.ofPattern("yyyy, MM, dd");

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private UserService userService;
    @Mock
    private AccommodationService accommodationService;
    @Mock
    private BookingBot bookingBot;
    @InjectMocks
    private BookingServiceImpl bookingService;
    private LocalDate date = LocalDate.now();

    @Test
    @DisplayName("Create booking with non exists booking from user and valid data, "
            + "should return BookingResponseDto")
    void createBooking_WithNonExistsBookingFromUserAndValidData_ShouldReturnBookingDto() {
        //Given
        Accommodation accommodation = createAccommodation(1L);
        BookingRequestDto requestDto = createBookingRequest(date.format(PATTERN_OF_DATE), 2, 1L);
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                requestDto.getCheckInDateYearMonthDay()
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(requestDto.getDaysOfStay())
                .minusSeconds(1);
        User user = createUser(User.Role.ROLE_MANAGER);
        Booking booking = createBooking(
                null, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        Booking savedBooking = createBooking(
                1L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        BookingResponseDto expected = createBookingResponse(savedBooking);
        Pageable pageable = PageRequest.of(0, 1);
        Page<Booking> bookings = Page.empty();

        //When
        when(userService.getAutnenticatedUser()).thenReturn(user);
        when(userService.existsById(user.getId())).thenReturn(true);
        when(bookingRepository.findByUserIdAndStatus(user.getId(), booking.getStatus(), pageable))
                .thenReturn(bookings);
        when(accommodationService.getAccommodationById(accommodation.getId()))
                .thenReturn(accommodation);
        when(bookingRepository.findAllBetweenCheckInDateAndCheckOutDate(
                checkInDate,
                checkOutDate,
                accommodation.getId()))
                .thenReturn(Collections.emptyList());
        when(bookingMapper.toEntity(requestDto, checkInDate, checkOutDate)).thenReturn(booking);
        when(bookingRepository.save(booking)).thenReturn(savedBooking);
        when(bookingMapper.toDto(savedBooking)).thenReturn(expected);

        //Then
        BookingResponseDto actual = bookingService.createBooking(requestDto);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        verify(userService, times(2)).getAutnenticatedUser();
        verify(userService, times(1)).existsById(user.getId());
        verify(bookingRepository, times(1))
                .findByUserIdAndStatus(user.getId(), booking.getStatus(), pageable);
        verify(accommodationService, times(1))
                .getAccommodationById(accommodation.getId());
        verify(bookingRepository, times(1)).findAllBetweenCheckInDateAndCheckOutDate(
                checkInDate, checkOutDate, accommodation.getId()
        );
        verify(bookingMapper, times(1)).toEntity(requestDto, checkInDate, checkOutDate);
        verify(bookingRepository, times(1)).save(booking);
        verify(bookingMapper, times(2)).toDto(savedBooking);
    }

    @Test
    @DisplayName("Create booking with past date, should return exception")
    void createBooking_WithPastDate_ShouldReturnException() {
        //Given
        BookingRequestDto requestDto = createBookingRequest("2024, 01, 19", 2, 1L);
        User user = createUser(User.Role.ROLE_MANAGER);
        Pageable pageable = PageRequest.of(0, 1);
        Page<Booking> bookings = Page.empty();

        //When
        when(userService.getAutnenticatedUser()).thenReturn(user);
        when(userService.existsById(user.getId())).thenReturn(true);
        when(bookingRepository.findByUserIdAndStatus(
                user.getId(), Booking.Status.PENDING, pageable)
        ).thenReturn(bookings);

        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> bookingService.createBooking(requestDto)
        );

        //Then
        LocalDate localDate = LocalDate.parse(
                requestDto.getCheckInDateYearMonthDay(), PATTERN_OF_DATE
        );
        String expected = "The date is incorrect: " + localDate
                + " , please enter a date greater than the current one: ";
        String actual = exception.getMessage();
        assertTrue(actual.contains(expected));

        verify(userService, times(1)).getAutnenticatedUser();
        verify(userService, times(1)).existsById(user.getId());
        verify(bookingRepository, times(1)).findByUserIdAndStatus(
                user.getId(), Booking.Status.PENDING, pageable);
    }

    @Test
    @DisplayName("Create booking with exists booking with status pending should return exception")
    void createBooking_WithExistsBookingWithStatusPending_ShouldReturnException() {
        //Given
        BookingRequestDto requestDto = createBookingRequest(date.format(PATTERN_OF_DATE), 2, 1L);
        User user = createUser(User.Role.ROLE_MANAGER);
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                requestDto.getCheckInDateYearMonthDay()
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(requestDto.getDaysOfStay())
                .minusSeconds(1);
        Accommodation accommodation = createAccommodation(1L);
        Booking booking = createBooking(
                1L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        List<Booking> bookingsList = List.of(booking);
        Pageable pageable = PageRequest.of(0, 1);
        Page<Booking> bookingPage = new PageImpl<>(bookingsList);

        //When
        when(userService.getAutnenticatedUser()).thenReturn(user);
        when(userService.existsById(user.getId())).thenReturn(true);
        when(bookingRepository.findByUserIdAndStatus(
                user.getId(), Booking.Status.PENDING, pageable)
        ).thenReturn(bookingPage);

        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> bookingService.createBooking(requestDto)
        );

        //Then
        String expected = "It is not possible to create a new booking until "
                + "you have paid or canceled the previous booking.";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        verify(userService, times(1)).getAutnenticatedUser();
        verify(userService, times(1)).existsById(user.getId());
        verify(bookingRepository, times(1))
                .findByUserIdAndStatus(user.getId(), Booking.Status.PENDING, pageable);
    }

    @Test
    @DisplayName("Create booking with accommodation availability 0, should return exception")
    void createBooking_WithAccommodationAvailabilityZero_ShouldReturnException() {
        //Given
        Accommodation accommodation = createAccommodation(1L);
        accommodation.setAvailability(0);
        BookingRequestDto requestDto = createBookingRequest(date.format(PATTERN_OF_DATE), 2, 1L);
        User user = createUser(User.Role.ROLE_MANAGER);
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                requestDto.getCheckInDateYearMonthDay()
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(requestDto.getDaysOfStay())
                .minusSeconds(1);
        Pageable pageable = PageRequest.of(0, 1);
        Page<Booking> bookings = Page.empty();

        //When
        when(userService.getAutnenticatedUser()).thenReturn(user);
        when(userService.existsById(user.getId())).thenReturn(true);
        when(bookingRepository.findByUserIdAndStatus(
                user.getId(), Booking.Status.PENDING, pageable)
        ).thenReturn(bookings);
        when(accommodationService.getAccommodationById(1L)).thenReturn(accommodation);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> bookingService.createBooking(requestDto)
        );

        //Then
        String expected = "Unable to booking this property, availability is: 0";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        verify(userService, times(1)).getAutnenticatedUser();
        verify(userService, times(1)).existsById(user.getId());
        verify(bookingRepository, times(1))
                .findByUserIdAndStatus(user.getId(), Booking.Status.PENDING, pageable);
        verify(accommodationService, times(1)).getAccommodationById(1L);
    }

    @Test
    @DisplayName("Create booking with non availability for check in date, should return exception")
    void createBooking_WithNonAvailabilityForDate_ShouldReturnException() {
        //Given
        Accommodation accommodation = createAccommodation(1L);
        BookingRequestDto requestDto = createBookingRequest(date.format(PATTERN_OF_DATE), 2, 1L);
        User user = createUser(User.Role.ROLE_MANAGER);
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                requestDto.getCheckInDateYearMonthDay()
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(requestDto.getDaysOfStay())
                .minusSeconds(1);
        Pageable pageable = PageRequest.of(0, 1);
        Page<Booking> bookings = Page.empty();
        Booking bookingPending = createBooking(
                1L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        Booking bookingConfirmed = createBooking(
                1L, user, checkInDate, checkOutDate, accommodation, Booking.Status.CONFIRMED
        );
        List<Booking> allByCheckDateInAndOut = List.of(bookingPending, bookingConfirmed);

        //When
        when(userService.getAutnenticatedUser()).thenReturn(user);
        when(userService.existsById(user.getId())).thenReturn(true);
        when(bookingRepository.findByUserIdAndStatus(
                user.getId(), Booking.Status.PENDING, pageable)
        ).thenReturn(bookings);
        when(accommodationService.getAccommodationById(1L)).thenReturn(accommodation);
        when(bookingRepository.findAllBetweenCheckInDateAndCheckOutDate(
                checkInDate, checkOutDate, accommodation.getId()))
                .thenReturn(allByCheckDateInAndOut);

        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> bookingService.createBooking(requestDto)
        );

        //Then
        String expected = "There are no vacancies in the interval from: "
                + checkInDate + ", to: " + checkOutDate;
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        verify(userService, times(1)).getAutnenticatedUser();
        verify(userService, times(1)).existsById(user.getId());
        verify(bookingRepository, times(1)).findByUserIdAndStatus(
                user.getId(), Booking.Status.PENDING, pageable);
        verify(accommodationService, times(1)).getAccommodationById(1L);
        verify(bookingRepository, times(1)).findAllBetweenCheckInDateAndCheckOutDate(
                checkInDate, checkOutDate, accommodation.getId()
        );
    }

    @Test
    @DisplayName("Get all by valid user id and valid status should return all BookingResponseDto")
    void getAll_ByValidUserIdAndValidStatus_ShouldReturnListBookingResponseDto() {
        //Given
        User user = createUser(User.Role.ROLE_MANAGER);
        Booking.Status status = Booking.Status.PENDING;
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                date.plusDays(2).format(PATTERN_OF_DATE)
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(2)
                .minusSeconds(1);
        Accommodation accommodation = createAccommodation(1L);
        Pageable pageable = PageRequest.of(0, 10);
        Booking booking1 = createBooking(
                1L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        Booking booking2 = createBooking(
                2L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        Page<Booking> bookingModels = new PageImpl<>(List.of(booking1, booking2));
        List<BookingResponseDto> expected = new ArrayList<>();

        //When
        when(userService.existsById(user.getId())).thenReturn(true);
        when(bookingRepository.findByUserIdAndStatus(user.getId(), status, pageable))
                .thenReturn(bookingModels);
        for (Booking booking : bookingModels) {
            BookingResponseDto responseDto = createBookingResponse(booking);
            expected.add(responseDto);
            when(bookingMapper.toDto(booking)).thenReturn(responseDto);
        }

        //Then
        List<BookingResponseDto> actual = bookingService.getAllByUserIdAndStatus(
                user.getId(), status.name(), pageable
        );
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));

        verify(userService, times(1)).existsById(user.getId());
        verify(bookingRepository, times(1))
                .findByUserIdAndStatus(user.getId(), status, pageable);
        verify(bookingMapper, times(actual.size())).toDto(any(Booking.class));
    }

    @Test
    @DisplayName("Get all by invalid user id and valid status, should return exception")
    void getAll_ByInvalidUserIdAndValidStatus_ShouldReturnException() {
        //Given
        Long userId = 999L;
        Booking.Status status = Booking.Status.PENDING;
        Pageable pageable = PageRequest.of(0, 10);

        //When
        when(userService.existsById(userId)).thenReturn(false);
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> bookingService.getAllByUserIdAndStatus(userId, status.name(), pageable)
        );

        //Then
        String expected = "Can't find user by id: " + userId;
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        verify(userService, times(1)).existsById(userId);
    }

    @Test
    @DisplayName("Get all by valid user id and invalid status, should return exception")
    void getAll_ByValidUserIdAndInvalidStatus_ShouldReturnException() {
        //Given
        String status = "INVALID";
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        //When
        when(userService.existsById(userId)).thenReturn(true);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> bookingService.getAllByUserIdAndStatus(userId, status, pageable)
        );

        //Then
        String expected = "This status does not exist or the data is not entered correctly";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        verify(userService, times(1)).existsById(userId);
    }

    @Test
    @DisplayName("Get all my bookings, should return all my bookings")
    void getAll_ForMyBooking_ShouldReturnAllMyBookingResponseDto() {
        //Given
        User user = createUser(User.Role.ROLE_CUSTOMER);
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                date.plusDays(2).format(PATTERN_OF_DATE)
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(2)
                .minusSeconds(1);
        Accommodation accommodation = createAccommodation(1L);
        Booking booking1 = createBooking(
                1L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        Booking booking2 = createBooking(
                2L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        Page<Booking> bookingPage = new PageImpl<>(List.of(booking1, booking2));
        List<BookingResponseDto> expected = new ArrayList<>();

        //When
        when(userService.getAutnenticatedUser()).thenReturn(user);
        when(bookingRepository.findAllByUser(user, pageable)).thenReturn(bookingPage);
        for (Booking booking : bookingPage) {
            BookingResponseDto bookingResponse = createBookingResponse(booking);
            expected.add(bookingResponse);
            when(bookingMapper.toDto(booking)).thenReturn(bookingResponse);
        }

        //Then
        List<BookingResponseDto> actual = bookingService.getAllMyBookings(pageable);
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));

        verify(userService, times(1)).getAutnenticatedUser();
        verify(bookingRepository, times(1)).findAllByUser(user, pageable);
        verify(bookingMapper, times(actual.size())).toDto(any(Booking.class));
    }

    @Test
    @DisplayName("Get by id by valid id should return BookingResponseDto")
    void getById_ByValidId_ShouldReturnBookingResponseDto() {
        //Given
        User user = createUser(User.Role.ROLE_CUSTOMER);
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                date.plusDays(2).format(PATTERN_OF_DATE)
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(2)
                .minusSeconds(1);
        Accommodation accommodation = createAccommodation(1L);
        Booking booking = createBooking(
                1L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        BookingResponseDto expected = createBookingResponse(booking);

        //When
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(expected);

        //Then
        BookingResponseDto actual = bookingService.getById(booking.getId());
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        verify(bookingRepository, times(1)).findById(booking.getId());
        verify(bookingMapper, times(1)).toDto(booking);
    }

    @Test
    @DisplayName("Get by id by invalid id, should return exception")
    void getById_ByInvalidId_ShouldReturnException() {
        //Given
        Long id = 99L;

        //When
        when(bookingRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> bookingService.getById(id)
        );

        //Then
        String expected = "Can't find booking by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(bookingRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Delete by id by valid id, should dont throw exception")
    void deleteById_ByValidId_ShouldDontThrowException() {
        //Given
        Long id = 1L;
        User user = createUser(User.Role.ROLE_CUSTOMER);
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                date.plusDays(2).format(PATTERN_OF_DATE)
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(2)
                .minusSeconds(1);
        Accommodation accommodation = createAccommodation(1L);
        Booking booking = createBooking(
                1L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        //When
        when(bookingRepository.findById(id)).thenReturn(Optional.of(booking));
        when(userService.getAutnenticatedUser()).thenReturn(user);

        //Then
        assertDoesNotThrow(() -> bookingService.deleteById(booking.getId()));
        verify(bookingRepository, times(1)).findById(booking.getId());
        verify(userService, times(1)).getAutnenticatedUser();
    }

    @Test
    @DisplayName("Delete by id by incorrect user, should return exception")
    void deleteById_ByIncorrectUser_ShouldReturnException() {
        //Given
        Long bookingId = 1L;
        User userManager = createUser(User.Role.ROLE_MANAGER);
        User userCustomer = createUser(User.Role.ROLE_CUSTOMER);
        userCustomer.setId(2L);

        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                date.plusDays(2).format(PATTERN_OF_DATE)
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(2)
                .minusSeconds(1);
        Accommodation accommodation = createAccommodation(1L);
        Booking booking = createBooking(
                1L, userManager, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );
        //When
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userService.getAutnenticatedUser()).thenReturn(userCustomer);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> bookingService.deleteById(bookingId)
        );

        //Then
        String expected = "Unable to process booking by id: " + booking.getId()
                + ", it is not yours";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        verify(bookingRepository, times(1)).findById(bookingId);
        verify(userService, times(1)).getAutnenticatedUser();
    }

    @Test
    void updateById_WithValidIdAndNotNullRequestData_ShouldUpdateBooking() {
        //Given
        User user = createUser(User.Role.ROLE_CUSTOMER);
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(
                date.plusDays(2).format(PATTERN_OF_DATE)
        );
        LocalDateTime checkOutDate = checkInDate.plusDays(2)
                .minusSeconds(1);
        Accommodation accommodation = createAccommodation(1L);

        BookingUpdateRequestDto requestDto = new BookingUpdateRequestDto();
        requestDto.setStatus("CONFIRMED");
        requestDto.setDaysOfStay(3);
        requestDto.setCheckInDateYearMonthDay(date.plusDays(5).format(PATTERN_OF_DATE));

        List<Booking> allByCheckDateInAndOut = Collections.emptyList();

        LocalDateTime updatedCheckInDate = checkAndParseCheckInDateToLocalDateTime(
                requestDto.getCheckInDateYearMonthDay()
        );
        LocalDateTime updatedCheckOutDate = updatedCheckInDate.plusDays(requestDto.getDaysOfStay())
                .minusSeconds(1);
        Booking updatedBooking = createBooking(
                1L,
                user,
                updatedCheckInDate,
                updatedCheckOutDate,
                accommodation,
                Booking.Status.PENDING
        );
        BookingResponseDto responseDto = createBookingResponse(updatedBooking);
        Booking booking = createBooking(
                1L, user, checkInDate, checkOutDate, accommodation, Booking.Status.PENDING
        );

        //When
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userService.getAutnenticatedUser()).thenReturn(user);
        when(bookingRepository.findAllBetweenCheckInDateAndCheckOutDate(
                updatedCheckInDate, updatedCheckOutDate, accommodation.getId()))
                .thenReturn(allByCheckDateInAndOut);
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(responseDto);
        when(bookingRepository.save(any(Booking.class))).thenReturn(updatedBooking);

        //Then
        assertDoesNotThrow(() -> bookingService.updateById(booking.getId(), requestDto));
    }

    private User createUser(User.Role role) {
        User user = new User();
        user.setId(1L);
        user.setEmail("customer@example.com");
        user.setPassword("User=123456789");
        user.setFirstName("Bob");
        user.setLastName("Alison");
        user.setRole(role);
        return user;
    }

    private BookingResponseDto createBookingResponse(Booking booking) {
        return new BookingResponseDto(
                booking.getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getAccommodation().getId(),
                booking.getUser().getId(),
                booking.getStatus().name()
        );
    }

    private Booking createBooking(
            Long id,
            User user,
            LocalDateTime checkInDate,
            LocalDateTime checkOutDate,
            Accommodation accommodation,
            Booking.Status status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
        booking.setAccommodation(accommodation);
        booking.setUser(user);
        booking.setStatus(status);
        return booking;
    }

    private BookingRequestDto createBookingRequest(
            String checkInDate, Integer dayOfStay, Long accommodationId
    ) {
        BookingRequestDto bookingRequestDto = new BookingRequestDto();
        bookingRequestDto.setCheckInDateYearMonthDay(checkInDate);
        bookingRequestDto.setDaysOfStay(dayOfStay);
        bookingRequestDto.setAccommodationId(accommodationId);
        return bookingRequestDto;
    }

    private Accommodation createAccommodation(Long id) {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(id);
        accommodation.setType(Accommodation.Type.APARTMENT);
        accommodation.setSize(Accommodation.Size.TWO_BEDROOM);
        accommodation.setAddress(createAddress("Odessa, Bocharova, 28"));
        accommodation.setAmenities(createAmenities(Set.of("WIFI", "PARKING")));
        accommodation.setDailyRate(BigDecimal.valueOf(100));
        accommodation.setAvailability(1);
        return accommodation;
    }

    private Address createAddress(String address) {
        Address finalAddress = new Address();
        finalAddress.setId(1L);
        finalAddress.setAddress(address);
        return finalAddress;
    }

    private Set<Amenity> createAmenities(Set<String> amenityNames) {
        long id = 1L;
        Set<Amenity> amenities = new HashSet<>();

        for (String name : amenityNames) {
            Amenity amenity = new Amenity();
            amenity.setId(id);
            amenity.setName(name);
            amenities.add(amenity);
            id++;
        }
        return amenities;
    }

    private LocalDateTime checkAndParseCheckInDateToLocalDateTime(String date) {
        LocalDate localDate = LocalDate.parse(date, PATTERN_OF_DATE);

        return localDate.atTime(12, 0, 0);
    }
}
