package booking.app.service.accommodation;

import booking.app.dto.accommodation.AccommodationFullInfoResponseDto;
import booking.app.dto.accommodation.AccommodationIncompleteInfoResponseDto;
import booking.app.dto.accommodation.AccommodationRequestDto;
import booking.app.dto.accommodation.AccommodationUpdateRequestDto;
import booking.app.exception.DataProcessingException;
import booking.app.exception.EntityNotFoundException;
import booking.app.mapper.AccommodationMapper;
import booking.app.model.accommodation.Accommodation;
import booking.app.model.accommodation.Accommodation.Size;
import booking.app.model.accommodation.Accommodation.Type;
import booking.app.model.accommodation.Address;
import booking.app.model.accommodation.Amenity;
import booking.app.repository.AccommodationRepository;
import booking.app.service.accommodation.address.AddressService;
import booking.app.service.accommodation.amenity.AmenityService;
import booking.app.telegram.BookingBot;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {
    private static final String[] HOUSE_TYPES_WITH_ONE_ADDRESS_AND_AVAILABILITY =
            new String[] {"HOUSE", "VACATION_HOME", "COTTAGE", "TOWNHOUSE"};

    private final AccommodationMapper accommodationMapper;
    private final AmenityService amenityService;
    private final AddressService addressService;
    private final AccommodationRepository accommodationRepository;
    private final BookingBot bookingBot;

    @Override
    @Transactional
    public AccommodationFullInfoResponseDto createAccommodation(AccommodationRequestDto request) {
        Type type = checkAndGetSizeOrType(request.getType(), Type.class);
        checkAvailabilityForHouseType(type, request.getAvailability());
        checkValidAddressForHouseType(type, request.getAddress());
        Size size = checkAndGetSizeOrType(request.getSize(), Size.class);
        Accommodation accommodation = accommodationMapper.toEntity(request);
        accommodation.setAmenities(amenityService.getSetAmenitiesByAmenitiesNames(
                request.getAmenities())
        );
        accommodation.setAddress(
                addressService.getAddressIfExistingOrSaveAndGet(request.getAddress())
        );
        accommodation.setType(type);
        accommodation.setSize(size);

        Accommodation checkedAccommodation = checkAndReturnExistingAccommodation(accommodation);

        if (checkedAccommodation != null) {
            checkedAccommodation.setAvailability(
                    checkedAccommodation.getAvailability() + request.getAvailability()
            );
            bookingBot.handleIncomingMessage("Update accommodation availability |"
                            + System.lineSeparator()
                    + accommodationMapper.toFullDto(checkedAccommodation).toString()
            );
        } else {
            accommodation = accommodationRepository.save(accommodation);
            bookingBot.handleIncomingMessage("Created new accommodation |"
                    + System.lineSeparator()
                    + accommodationMapper.toFullDto(accommodation).toString()
            );
        }

        return checkedAccommodation != null
                ? accommodationMapper.toFullDto(checkedAccommodation)
                : accommodationMapper.toFullDto(accommodation);
    }

    @Override
    public List<AccommodationIncompleteInfoResponseDto> getAll(final Pageable pageable) {
        return accommodationRepository.findAll(pageable).stream()
                .filter(accommodation -> accommodation.getAvailability() > 0)
                .map(accommodationMapper::toIncompleteDto)
                .toList();
    }

    @Override
    public AccommodationFullInfoResponseDto getById(final Long id) {
        return accommodationMapper.toFullDto(getAccommodationById(id));
    }

    @Override
    public void updateById(
            final Long id,
            final AccommodationUpdateRequestDto request
    ) {
        Accommodation accommodation = getAccommodationById(id);

        if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
            Set<Amenity> amenities = amenityService.getSetAmenitiesByAmenitiesNames(
                    request.getAmenities()
            );
            accommodation.setAmenities(amenities);
        }

        if (request.getAvailability() != null) {
            checkAvailabilityForHouseType(accommodation.getType(), request.getAvailability());
            accommodation.setAvailability(request.getAvailability());
        }

        if (request.getDailyRate() != null) {
            accommodation.setDailyRate(request.getDailyRate());
        }
        accommodationRepository.save(accommodation);
    }

    @Override
    public void deleteById(final Long id) {
        Accommodation accommodation = accommodationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find accommodation by id: " + id)
        );
        checkAddressForHouseTypeAndDelete(accommodation.getType(), accommodation.getAddress());
        accommodationRepository.deleteById(id);
    }

    @Override
    public Accommodation getAccommodationById(final Long id) {
        return accommodationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find accommodation by id: " + id)
        );
    }

    private void checkAddressForHouseTypeAndDelete(Type type, Address address) {
        for (String value : HOUSE_TYPES_WITH_ONE_ADDRESS_AND_AVAILABILITY) {
            if (type.name().equals(value)) {
                addressService.deleteById(address.getId());
            }
        }
    }

    private Accommodation checkAndReturnExistingAccommodation(Accommodation accommodation) {
        return accommodationRepository.findByTypeAndAddressIdAndSizeAndDailyRate(
                accommodation.getType(),
                accommodation.getAddress().getId(),
                accommodation.getSize(),
                accommodation.getDailyRate()
        ).orElse(null);
    }

    private void checkAvailabilityForHouseType(Type type, Integer availability) {
        for (String value : HOUSE_TYPES_WITH_ONE_ADDRESS_AND_AVAILABILITY) {
            if (type.name().equals(value) && availability > 1) {
                throw new DataProcessingException(
                        "There cannot be more than 1 availability for this type: " + type.name()
                                + " of accommodation"
                );
            }
        }
    }

    private void checkValidAddressForHouseType(Type type, String address) {
        if (!addressService.checkExistingAddress(address)) {
            return;
        }

        if (checkContainsTypeFromHouseTypes(type)) {
            throw new DataProcessingException("This address: " + address
                    + " already exists for another property of this type: " + type.name());
        }

        Address addressFromDb = addressService.getAddressByAddressArgument(address);
        List<Accommodation> accommodations = accommodationRepository.findByAddress(addressFromDb);

        for (Accommodation accommodation : accommodations) {
            if (checkContainsTypeFromHouseTypes(accommodation.getType())) {
                throw new DataProcessingException("This address: " + address
                        + " already exists for another property of this type: "
                        + accommodation.getType().name());
            }
        }
    }

    private boolean checkContainsTypeFromHouseTypes(Type type) {
        return Arrays.stream(
                HOUSE_TYPES_WITH_ONE_ADDRESS_AND_AVAILABILITY).toList().contains(type.name()
        );
    }

    private <T extends Enum<T>> T checkAndGetSizeOrType(String name, Class<T> enumType) {
        String validName = name.trim().contains(" ")
                ? name.toUpperCase().trim().replace(" ", "_")
                : name.toUpperCase().trim();

        for (T value : enumType.getEnumConstants()) {
            if (validName.equals(value.name())) {
                return value;
            }
        }

        throw new DataProcessingException(generateExceptionMessage(enumType));
    }

    private <T extends Enum<T>> String generateExceptionMessage(Class<T> enumType) {
        T[] enumConstants = enumType.getEnumConstants();
        StringBuilder builder =
                new StringBuilder("The entered data is incorrect, select one of these: ");

        for (int i = 0; i < enumConstants.length; i++) {
            builder.append(enumConstants[i].name());

            if (i < enumConstants.length - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }
}
