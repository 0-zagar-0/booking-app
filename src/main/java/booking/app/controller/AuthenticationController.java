package booking.app.controller;

import booking.app.dto.user.UserLoginRequestDto;
import booking.app.dto.user.UserLoginResponseDto;
import booking.app.dto.user.UserRegisterRequestDto;
import booking.app.dto.user.UserResponseDto;
import booking.app.exception.RegistrationException;
import booking.app.security.AuthenticationService;
import booking.app.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication management", description = "Endpoints for authorization users")
@RestController
@RequestMapping(value = "/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Register user")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@RequestBody @Valid UserRegisterRequestDto request)
            throws RegistrationException {
        return userService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Login user")
    @ResponseStatus(HttpStatus.OK)
    public UserLoginResponseDto login(@RequestBody UserLoginRequestDto request) {
        return authenticationService.authenticate(request);
    }
}
