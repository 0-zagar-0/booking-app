package booking.app.mapper;

import booking.app.config.MapperConfig;
import booking.app.dto.booking.BookingRequestDto;
import booking.app.dto.booking.BookingResponseDto;
import booking.app.model.Booking;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface BookingMapper {
    @Mapping(target = "accommodationId", source = "booking.accommodation.id")
    @Mapping(target = "userId", source = "booking.user.id")
    BookingResponseDto toDto(Booking booking);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "accommodation", ignore = true)
    @Mapping(target = "checkInDate", source = "checkInDate")
    @Mapping(target = "checkOutDate", source = "checkOutDate")
    @Mapping(target = "status", expression = "java(Booking.Status.PENDING)")
    Booking toEntity(
            BookingRequestDto request,
            LocalDateTime checkInDate,
            LocalDateTime checkOutDate
    );
}
