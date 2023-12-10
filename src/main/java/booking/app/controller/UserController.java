package booking.app.controller;

import booking.app.dto.user.UserResponseDto;
import booking.app.dto.user.UserUpdateProfileInformationDto;
import booking.app.dto.user.UserUpdateRoleDto;
import booking.app.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User management", description = "Endpoints for users actions")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_MANAGER')")
    @Operation(summary = "Get user profile", description = "Get user profile")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDto getUserProfile() {
        return userService.getUserProfile();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_MANAGER')")
    @Operation(summary = "Update user role", description = "Update user role by user id")
    @ResponseStatus(HttpStatus.OK)
    public void updateUserRole(
            @PathVariable Long id, @RequestBody UserUpdateRoleDto updateRoleDto
    ) {
        userService.updateUserRole(id, updateRoleDto);
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_MANAGER')")
    @Operation(summary = "Update user profile", description = "Update user profile")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDto updateUserProfile(@RequestBody UserUpdateProfileInformationDto request) {
        return userService.updateUserProfile(request);
    }

}
