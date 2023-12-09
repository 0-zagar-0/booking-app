package booking.app.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserLoginRequestDto {
    @NotBlank
    @Size(min = 8, max = 30)
    private String email;
    @NotBlank
    @Size(min = 8, max = 50)
    private String password;
}
