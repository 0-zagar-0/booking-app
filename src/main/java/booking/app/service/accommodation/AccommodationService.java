package booking.app.service.accommodation;

import booking.app.dto.accommodation.AccommodationFullInfoResponseDto;
import booking.app.dto.accommodation.AccommodationIncompleteInfoResponseDto;
import booking.app.dto.accommodation.AccommodationRequestDto;
import booking.app.dto.accommodation.AccommodationUpdateRequestDto;
import booking.app.model.accommodation.Accommodation;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface AccommodationService {
    AccommodationFullInfoResponseDto createAccommodation(AccommodationRequestDto request);

    List<AccommodationIncompleteInfoResponseDto> getAll(Pageable pageable);

    AccommodationFullInfoResponseDto getById(Long id);

    void updateById(Long id, AccommodationUpdateRequestDto request);

    void deleteById(Long id);

    Accommodation getAccommodationById(Long id);
}
