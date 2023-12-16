package booking.app.controller;

import booking.app.dto.accommodation.AccommodationFullInfoResponseDto;
import booking.app.dto.accommodation.AccommodationIncompleteInfoResponseDto;
import booking.app.dto.accommodation.AccommodationRequestDto;
import booking.app.dto.accommodation.AccommodationUpdateRequestDto;
import booking.app.service.accommodation.AccommodationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Accommodation management", description = "Endpoints for accommodations action")
@RestController
@RequestMapping("/accommodations")
@RequiredArgsConstructor
public class AccommodationController {
    private final AccommodationService accommodationService;

    @PostMapping
    @Operation(summary = "Add accommodation", description = "Added accommodation")
    @ResponseStatus(HttpStatus.CREATED)
    public AccommodationFullInfoResponseDto createAccommodation(
            @RequestBody @Valid AccommodationRequestDto request
    ) {
        return accommodationService.createAccommodation(request);
    }

    @GetMapping
    @Operation(summary = "Get all", description = "Get all accommodations of availability")
    @ResponseStatus(HttpStatus.OK)
    public List<AccommodationIncompleteInfoResponseDto> getAll(Pageable pageable) {
        return accommodationService.getAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get by id", description = "Get full information about accommodation")
    @ResponseStatus(HttpStatus.OK)
    public AccommodationFullInfoResponseDto getById(@PathVariable Long id) {
        return accommodationService.getById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update by id", description = "Update accommodation by id")
    @ResponseStatus(HttpStatus.OK)
    public void updateById(
            @PathVariable Long id,
            @RequestBody AccommodationUpdateRequestDto request
    ) {
        accommodationService.updateById(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete by id", description = "Delete accommodation by id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        accommodationService.deleteById(id);
    }
}
