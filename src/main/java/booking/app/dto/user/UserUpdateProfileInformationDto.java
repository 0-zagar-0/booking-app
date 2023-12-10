package booking.app.dto.user;

public record UserUpdateProfileInformationDto(
        String email,
        String password,
        String firstName,
        String lastName
) {
}
