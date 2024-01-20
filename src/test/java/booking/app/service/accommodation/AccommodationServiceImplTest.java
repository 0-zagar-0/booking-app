package booking.app.service.accommodation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import booking.app.dto.accommodation.AccommodationFullInfoResponseDto;
import booking.app.dto.accommodation.AccommodationIncompleteInfoResponseDto;
import booking.app.dto.accommodation.AccommodationRequestDto;
import booking.app.dto.accommodation.AccommodationUpdateRequestDto;
import booking.app.exception.DataProcessingException;
import booking.app.exception.EntityNotFoundException;
import booking.app.mapper.AccommodationMapper;
import booking.app.model.accommodation.Accommodation;
import booking.app.model.accommodation.Address;
import booking.app.model.accommodation.Amenity;
import booking.app.repository.AccommodationRepository;
import booking.app.service.accommodation.address.AddressService;
import booking.app.service.accommodation.amenity.AmenityService;
import booking.app.telegram.BookingBot;
import java.math.BigDecimal;
import java.util.ArrayList;
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
class AccommodationServiceImplTest {
    @Mock
    private AccommodationMapper accommodationMapper;
    @Mock
    private AmenityService amenityService;
    @Mock
    private AddressService addressService;
    @Mock
    private AccommodationRepository accommodationRepository;
    @Mock
    private BookingBot bookingBot;
    @InjectMocks
    private AccommodationServiceImpl accommodationService;

    @Test
    @DisplayName("Create accommodation with valid data and non exists accommodation, "
            + "should return accommodation dto")
    void createAccommodation_WithValidDataAndNonExistsAccommodation_ShouldReturnAccommodationDto() {
        //Given
        AccommodationRequestDto request = createRequestDto();
        Accommodation accommodationWithoutId = createAccommodation(request);
        Accommodation accommodationWithId = createAccommodation(request);
        accommodationWithId.setId(1L);
        AccommodationFullInfoResponseDto expected =
                createAccommodationFullDto(accommodationWithId, request);
        Accommodation checkAccommodation = null;

        //When
        when(accommodationMapper.toEntity(request)).thenReturn(accommodationWithoutId);
        when(addressService.getAddressIfExistingOrSaveAndGet(
                accommodationWithoutId.getAddress().getAddress()))
                .thenReturn(accommodationWithoutId.getAddress());
        when(accommodationRepository.findByTypeAndAddressIdAndSizeAndDailyRate(
                accommodationWithoutId.getType(),
                accommodationWithoutId.getAddress().getId(),
                accommodationWithoutId.getSize(),
                accommodationWithoutId.getDailyRate()
        )).thenReturn(Optional.ofNullable(checkAccommodation));
        when(accommodationRepository.save(accommodationWithoutId)).thenReturn(accommodationWithId);
        when(accommodationMapper.toFullDto(accommodationWithId)).thenReturn(expected);
        when(amenityService.getSetAmenitiesByAmenitiesNames(
                request.getAmenities())).thenReturn(accommodationWithoutId.getAmenities()
        );

        //Then
        AccommodationFullInfoResponseDto actual = accommodationService.createAccommodation(request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        verify(accommodationMapper, times(1)).toEntity(request);
        verify(addressService, times(1))
                .getAddressIfExistingOrSaveAndGet(accommodationWithoutId.getAddress().getAddress());
        verify(accommodationRepository, times(1))
                .findByTypeAndAddressIdAndSizeAndDailyRate(
                        accommodationWithoutId.getType(),
                        accommodationWithoutId.getAddress().getId(),
                        accommodationWithoutId.getSize(),
                        accommodationWithoutId.getDailyRate()
                );
        verify(accommodationRepository, times(1))
                .save(accommodationWithoutId);
        verify(accommodationMapper, times(2)).toFullDto(accommodationWithId);
        verify(amenityService, times(1))
                .getSetAmenitiesByAmenitiesNames(request.getAmenities());
    }

    @Test
    @DisplayName("Create accommodation with valid data and exists accommodation, "
            + "should return accommodation dto")
    void createAccommodation_WithValidDataAndExistsAccommodation_ShouldReturnAccommodationDto() {
        //Given
        AccommodationRequestDto request = createRequestDto();
        Accommodation accommodationWithoutId = createAccommodation(request);

        Accommodation accommodationWithId = createAccommodation(request);
        accommodationWithId.setId(1L);
        accommodationWithId.setDailyRate(
                accommodationWithId.getDailyRate().add(request.getDailyRate())
        );
        Accommodation checkAccommodation = accommodationWithId;
        AccommodationFullInfoResponseDto expected =
                createAccommodationFullDto(accommodationWithId, request);

        //When
        when(accommodationMapper.toEntity(request)).thenReturn(accommodationWithoutId);
        when(addressService.getAddressIfExistingOrSaveAndGet(
                accommodationWithoutId.getAddress().getAddress()))
                .thenReturn(accommodationWithoutId.getAddress());
        when(accommodationRepository.findByTypeAndAddressIdAndSizeAndDailyRate(
                accommodationWithoutId.getType(),
                accommodationWithoutId.getAddress().getId(),
                accommodationWithoutId.getSize(),
                accommodationWithoutId.getDailyRate()
        )).thenReturn(Optional.of(checkAccommodation));
        when(accommodationMapper.toFullDto(accommodationWithId)).thenReturn(expected);

        //Then
        AccommodationFullInfoResponseDto actual = accommodationService.createAccommodation(request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        verify(accommodationMapper, times(1)).toEntity(request);
        verify(addressService, times(1))
                .getAddressIfExistingOrSaveAndGet(accommodationWithoutId.getAddress().getAddress());
        verify(accommodationRepository, times(1))
                .findByTypeAndAddressIdAndSizeAndDailyRate(
                        accommodationWithoutId.getType(),
                        accommodationWithoutId.getAddress().getId(),
                        accommodationWithoutId.getSize(),
                        accommodationWithoutId.getDailyRate()
                );
        verify(accommodationMapper, times(2)).toFullDto(accommodationWithId);
    }

    @Test
    @DisplayName("Create accommodation with valid availability for house type HOUSE,"
            + " should return exception")
    void createAccommodation_WithInvalidAvailabilityForHouseTypeHouse_ShouldReturnException() {
        //Given
        AccommodationRequestDto request = createRequestDto();
        request.setType("HOUSE");
        request.setAvailability(10);

        //When
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> accommodationService.createAccommodation(request)
        );

        //Then
        String expected = "There cannot be more than 1 availability for this type: "
                + request.getType() + " of accommodation";
        String actual = exception.getMessage();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Create accommodation with invalid type, should return exception")
    void createAccommodation_WithInvalidType_ShouldReturnException() {
        //Given
        AccommodationRequestDto request = createRequestDto();
        request.setType("Invalid type");

        //When
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> accommodationService.createAccommodation(request)
        );

        //Then
        String expected = "The entered data is incorrect, select one of these: ";
        String actual = exception.getMessage();
        assertTrue(actual.contains(expected));
    }

    @Test
    @DisplayName("Create accommodation with invalid size, should return exception")
    void createAccommodation_WithInvalidSize_ShouldReturnException() {
        //Given
        AccommodationRequestDto request = createRequestDto();
        request.setSize("three");

        //When
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> accommodationService.createAccommodation(request)
        );

        //Then
        String expected = "The entered data is incorrect, select one of these: ";
        String actual = exception.getMessage();
        assertTrue(actual.contains(expected));
    }

    @Test
    @DisplayName("Create accommodation with exists address for house type HOUSE, "
            + "should return exception")
    void createAccommodation_WithExistsAddressForTypeHouse_ShouldReturnException() {
        //Given
        AccommodationRequestDto request = createRequestDto();
        request.setType("HOUSE");

        //When
        when(addressService.checkExistingAddress(request.getAddress())).thenReturn(true);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> accommodationService.createAccommodation(request)
        );

        //Then
        String expected = "This address: " + request.getAddress()
                + " already exists for another property of this type: " + request.getType();
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(addressService, times(1)).checkExistingAddress(request.getAddress());
    }

    @Test
    @DisplayName("Create accommodation with exists address for type APARTMENT, "
            + "should return exception")
    void createAccommodation_WithExistsAddressForTypeApartment_ShouldReturnException() {
        //Given
        AccommodationRequestDto request = createRequestDto();
        Address address = new Address();
        address.setId(1L);
        address.setAddress(request.getAddress());

        Accommodation accommodation = new Accommodation();
        accommodation.setType(Accommodation.Type.HOUSE);
        accommodation.setSize(Accommodation.Size.TWO_BEDROOM);
        accommodation.setAddress(address);
        accommodation.setAmenities(createAmenities(request.getAmenities()));
        accommodation.setDailyRate(request.getDailyRate());
        accommodation.setAvailability(request.getAvailability());

        List<Accommodation> accommodations = List.of(accommodation);

        //When
        when(addressService.checkExistingAddress(request.getAddress())).thenReturn(true);
        when(addressService.getAddressByAddressArgument(request.getAddress())).thenReturn(address);
        when(accommodationRepository.findByAddress(address)).thenReturn(accommodations);

        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> accommodationService.createAccommodation(request)
        );

        //Then
        String expected = "This address: " + request.getAddress()
                + " already exists for another property of this type: "
                + accommodation.getType().name();
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        verify(addressService, times(1)).checkExistingAddress(request.getAddress());
        verify(addressService, times(1)).getAddressByAddressArgument(request.getAddress());
        verify(accommodationRepository, times(1)).findByAddress(address);
    }

    @Test
    @DisplayName("Get all with availability more zero, should return list accommodation dto")
    void getAll_WithAvailabilityMoreZero_ShouldReturnListAccommodationDto() {
        //Give
        List<Accommodation> accommodations =
                List.of(createAccommodation(1L), createAccommodation(2L));
        Page<Accommodation> accommodationsPage = new PageImpl<>(accommodations);
        List<AccommodationIncompleteInfoResponseDto> expected = new ArrayList<>();

        for (Accommodation accommodation : accommodations) {
            AccommodationIncompleteInfoResponseDto nonFullDto =
                    createAccommodationNonFullDto(accommodation);
            when(accommodationMapper.toIncompleteDto(accommodation)).thenReturn(nonFullDto);
            expected.add(nonFullDto);
        }

        //When
        Pageable pageable = PageRequest.of(0, 10);
        when(accommodationRepository.findAll(pageable)).thenReturn(accommodationsPage);

        //Then
        List<AccommodationIncompleteInfoResponseDto> actual = accommodationService.getAll(pageable);
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));

        verify(accommodationRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Get by id by valid id, should return accommodation full dto")
    void getById_ByValidId_ShouldReturnAccommodationFullDto() {
        //Given
        AccommodationRequestDto request = createRequestDto();
        Accommodation accommodation = createAccommodation(request);
        accommodation.setId(1L);
        AccommodationFullInfoResponseDto expected =
                createAccommodationFullDto(accommodation, request);

        //When
        when(accommodationRepository.findById(accommodation.getId()))
                .thenReturn(Optional.of(accommodation));
        when(accommodationMapper.toFullDto(accommodation)).thenReturn(expected);

        //Then
        AccommodationFullInfoResponseDto actual =
                accommodationService.getById(accommodation.getId());
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        verify(accommodationRepository, times(1)).findById(accommodation.getId());
        verify(accommodationMapper, times(1)).toFullDto(accommodation);
    }

    @Test
    @DisplayName("Get by id by invalid id, should return exception")
    void getById_ByInvalidId_ShouldReturnException() {
        //Given
        Long id = 999L;

        //When
        when(accommodationRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> accommodationService.getById(id)
        );

        //Then
        String expected = "Can't find accommodation by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        verify(accommodationRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Delete by id by valid id and type APARTMENT, should dont throw exception")
    void deleteById_ByValidIdAndTypeApartment_ShouldDontThrowException() {
        //Given
        Long id = 1L;
        Accommodation accommodation = createAccommodation(id);

        //When
        when(accommodationRepository.findById(id)).thenReturn(Optional.of(accommodation));

        //Then
        assertDoesNotThrow(() -> accommodationService.deleteById(id));

        verify(accommodationRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Delete by id by valid id type HOUSE, should dont throw exception")
    void deleteById_ByValidIdAndTypeHouse_ShouldDontThrowException() {
        //Given
        Long id = 1L;
        Accommodation accommodation = createAccommodation(id);
        accommodation.setType(Accommodation.Type.HOUSE);

        //When
        when(accommodationRepository.findById(id)).thenReturn(Optional.of(accommodation));

        //Then
        assertDoesNotThrow(() -> accommodationService.deleteById(id));

        verify(accommodationRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Delete by id by invalid id, should return exception")
    void deleteById_ByInvalidId_ShouldReturnException() {
        //Given
        Long id = 999L;

        //When
        when(accommodationRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> accommodationService.deleteById(id)
        );

        //Then
        String expected = "Can't find accommodation by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        verify(accommodationRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Update by id by valid id, should update accommodation")
    void updateById_ByValidId_ShouldUpdateAccommodation() {
        //Given
        Long id = 1L;
        AccommodationUpdateRequestDto request = new AccommodationUpdateRequestDto();
        request.setAmenities(Set.of("Big window"));
        request.setDailyRate(BigDecimal.valueOf(200));
        request.setAvailability(2);
        Accommodation accommodation = createAccommodation(id);

        //When
        when(accommodationRepository.findById(id)).thenReturn(Optional.of(accommodation));

        //Then
        assertDoesNotThrow(() -> accommodationService.updateById(id, request));
    }

    private AccommodationRequestDto createRequestDto() {
        AccommodationRequestDto request = new AccommodationRequestDto();
        request.setType("APARTMENT ");
        request.setAddress("City, Street, 10");
        request.setSize("TWO BEDROOM ");
        request.setAmenities(Set.of("WIFI", "PARKING"));
        request.setDailyRate(BigDecimal.valueOf(100));
        request.setAvailability(1);
        return request;
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

    private Accommodation createAccommodation(AccommodationRequestDto request) {
        Accommodation accommodation = new Accommodation();
        accommodation.setType(Accommodation.Type.APARTMENT);
        accommodation.setSize(Accommodation.Size.TWO_BEDROOM);
        accommodation.setAddress(createAddress(request.getAddress()));
        accommodation.setAmenities(createAmenities(request.getAmenities()));
        accommodation.setDailyRate(request.getDailyRate());
        accommodation.setAvailability(request.getAvailability());
        return accommodation;
    }

    private AccommodationIncompleteInfoResponseDto createAccommodationNonFullDto(
            Accommodation accommodation
    ) {
        return new AccommodationIncompleteInfoResponseDto(
                accommodation.getId(),
                accommodation.getType().name(),
                accommodation.getAddress().getAddress()
        );
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

    private AccommodationFullInfoResponseDto createAccommodationFullDto(
            Accommodation accommodation, AccommodationRequestDto requestDto
    ) {
        return new AccommodationFullInfoResponseDto(
                accommodation.getId(),
                accommodation.getType().name(),
                accommodation.getAddress().getAddress(),
                accommodation.getSize().name(),
                requestDto.getAmenities(),
                accommodation.getDailyRate(),
                accommodation.getAvailability()
        );
    }
}
