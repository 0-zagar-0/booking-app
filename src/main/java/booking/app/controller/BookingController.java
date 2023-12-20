package booking.app.controller;

import booking.app.dto.booking.BookingRequestDto;
import booking.app.dto.booking.BookingResponseDto;
import booking.app.dto.booking.BookingUpdateRequestDto;
import booking.app.service.booking.BookingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookings management", description = "Endpoints for bookings action")
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create booking", description = "Create booking")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponseDto createBooking(@RequestBody @Valid BookingRequestDto request) {
        return bookingService.createBooking(request);
    }

    @GetMapping
    @Operation(summary = "Get booking", description = "Get booking by user id and status")
    @ResponseStatus(HttpStatus.OK)
    public List<BookingResponseDto> getAllByIdAndStatus(
            @RequestParam(name = "user_id", required = true) Long userId,
            @RequestParam(name = "status", required = false) String status,
            Pageable pageable
    ) {
        return bookingService.getAllByUserIdAndStatus(userId, status, pageable);
    }

    @GetMapping("/me")
    @Operation(summary = "Get all my bookings", description = "Get all user bookings")
    @ResponseStatus(HttpStatus.OK)
    public List<BookingResponseDto> getAllMyBookings(Pageable pageable) {
        return bookingService.getAllMyBookings(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by id", description = "Get booking by id")
    @ResponseStatus(HttpStatus.OK)
    public BookingResponseDto getById(@PathVariable Long id) {
        return bookingService.getById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update by id", description = "Update by id")
    @ResponseStatus(HttpStatus.OK)
    public void updateById(@PathVariable Long id, @RequestBody BookingUpdateRequestDto request) {
        bookingService.updateById(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete by id", description = "Delete by id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        bookingService.deleteById(id);
    }


}
