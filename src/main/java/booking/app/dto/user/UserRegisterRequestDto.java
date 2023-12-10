package booking.app.dto.user;

import booking.app.validation.FieldMatch;
import booking.app.validation.Password;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@FieldMatch(field = "password",
        fieldMatch = "repeatPassword",
        message = "Passwords values don't match!")
public class UserRegisterRequestDto {
    @NotBlank
    @Size(min = 8, max = 30)
    private String email;
    @NotBlank
    @Size(min = 3, max = 20)
    private String firstName;
    @NotBlank
    @Size(min = 3, max = 20)
    private String lastName;
    @NotBlank
    @Password
    @Size(min = 8, max = 50)
    private String password;
    @NotBlank
    @Password
    @Size(min = 8, max = 50)
    private String repeatPassword;
}
