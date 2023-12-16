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

    @Override
    @Transactional
    public AccommodationFullInfoResponseDto createAccommodation(AccommodationRequestDto request) {
        Type type = checkAndGetSizeOrType(request.getType(), Type.class);
        checkAvailabilityForHouseType(type, request.getAvailability());
        checkAddressForHouseType(type, request.getAddress());

        Accommodation accommodation = accommodationMapper.toEntity(request);
        accommodation.setAmenities(amenityService.getSetAmenitiesByAmenitiesNames(
                request.getAmenities())
        );
        accommodation.setAddress(
                addressService.getAddressIfExistingOrSaveAndGet(request.getAddress())
        );
        accommodation.setType(type);
        accommodation.setSize(checkAndGetSizeOrType(request.getSize(), Size.class));

        Accommodation checkedAccommodation = checkAndReturnExistingAccommodation(accommodation);
        return checkedAccommodation != null
                ? accommodationMapper.toFullDto(checkedAccommodation)
                : accommodationMapper.toFullDto(accommodationRepository.save(accommodation));
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
        return accommodationMapper.toFullDto(accommodationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find accommodation by id: " + id))
        );
    }

    @Override
    public void updateById(
            final Long id,
            final AccommodationUpdateRequestDto request
    ) {
        Accommodation accommodation = accommodationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find accommodation by id: " + id)
        );

        if (!request.getAmenities().isEmpty()) {
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

    private void checkAddressForHouseType(Type type, String address) {
        for (String value : HOUSE_TYPES_WITH_ONE_ADDRESS_AND_AVAILABILITY) {
            if (type.name().equals(value) && addressService.checkExistingAddress(address)) {
                throw new DataProcessingException("This address: " + address
                        + " already exists for another property of this type: " + type.name());
            }
        }
    }

    private <T extends Enum<T>> T checkAndGetSizeOrType(String name, Class<T> enumType) {
        String newName = checkAndReturnValidString(name);

        for (T value : enumType.getEnumConstants()) {
            if (newName.equals(value.name())) {
                return value;
            }
        }

        throw new DataProcessingException(generateExceptionMessage(enumType));
    }

    private String checkAndReturnValidString(String value) {
        String newValue = value.toUpperCase().trim();

        if (value.contains(" ")) {
            return value.replace(' ', '_').toUpperCase();
        }

        return newValue;
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
