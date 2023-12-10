package booking.app.service.user;

import booking.app.dto.user.UserRegisterRequestDto;
import booking.app.dto.user.UserResponseDto;
import booking.app.dto.user.UserUpdateProfileInformationDto;
import booking.app.dto.user.UserUpdateRoleDto;
import booking.app.exception.RegistrationException;

public interface UserService {
    UserResponseDto register(UserRegisterRequestDto request) throws RegistrationException;

    UserResponseDto getUserProfile();

    void updateUserRole(Long id, UserUpdateRoleDto updateRoleDto);

    UserResponseDto updateUserProfile(UserUpdateProfileInformationDto request);
}
