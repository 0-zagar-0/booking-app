package booking.app.mapper;

import booking.app.config.MapperConfig;
import booking.app.dto.accommodation.AccommodationFullInfoResponseDto;
import booking.app.dto.accommodation.AccommodationIncompleteInfoResponseDto;
import booking.app.dto.accommodation.AccommodationRequestDto;
import booking.app.model.accommodation.Accommodation;
import booking.app.model.accommodation.Address;
import booking.app.model.accommodation.Amenity;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapperConfig.class)
public interface AccommodationMapper {
    @Mapping(target = "address", source = "address", qualifiedByName = "getAddressString")
    @Mapping(target = "amenities", source = "amenities", qualifiedByName = "setAmenitiesToString")
    AccommodationFullInfoResponseDto toFullDto(Accommodation accommodation);

    @Mapping(target = "address", source = "address", qualifiedByName = "getAddressString")
    AccommodationIncompleteInfoResponseDto toIncompleteDto(Accommodation accommodation);

    @Mapping(target = "type", ignore = true)
    @Mapping(target = "size", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "amenities", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Accommodation toEntity(AccommodationRequestDto request);

    @Named("setAmenitiesToString")
    default Set<String> setAmenitiesToString(Set<Amenity> amenities) {
        return amenities.stream()
                .map(Amenity::getName)
                .collect(Collectors.toSet());
    }

    @Named("getAddressString")
    default String getAddressString(Address address) {
        return address.getAddress();
    }
}
