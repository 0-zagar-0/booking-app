package booking.app.dto.user;

import booking.app.validation.Password;
import jakarta.validation.constraints.Email;

public record UserUpdateProfileInformationDto(
        @Email
        String email,
        @Password
        String password,
        String firstName,
        String lastName
) {
}
