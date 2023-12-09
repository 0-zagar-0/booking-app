package booking.app.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
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
    @Size(min = 8, max = 50)
    private String password;
    @NotBlank
    @Size(min = 8, max = 50)
    private String repeatPassword;
}
