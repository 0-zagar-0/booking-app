package booking.app.service.user;

import booking.app.dto.user.UserRegisterRequestDto;
import booking.app.dto.user.UserResponseDto;
import booking.app.dto.user.UserUpdateProfileInformationDto;
import booking.app.dto.user.UserUpdateRoleDto;
import booking.app.exception.DataProcessingException;
import booking.app.exception.EntityNotFoundException;
import booking.app.exception.RegistrationException;
import booking.app.mapper.UserMapper;
import booking.app.model.User;
import booking.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto register(final UserRegisterRequestDto request)
            throws RegistrationException {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RegistrationException("Can't complete registration");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto getUserProfile() {
        User user = getAutnenticatedUser();
        return userMapper.toDto(user);
    }

    @Override
    public void updateUserRole(final Long id, final UserUpdateRoleDto updateRoleDto) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find user by id: " + id)
        );
        final User.Role role = checkValidUserRole(updateRoleDto);
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    public UserResponseDto updateUserProfile(final UserUpdateProfileInformationDto request) {
        User user = getAutnenticatedUser();

        if (request.email() != null && !request.email().isEmpty()
                && !request.email().equals(user.getEmail())) {
            user.setEmail(request.email());
        }

        if (request.password() != null && !request.password().isEmpty()
                && !request.password().equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.firstName() != null && !request.firstName().isEmpty()
                && !request.firstName().equals(user.getFirstName())) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null && !request.lastName().isEmpty()
                && !request.lastName().equals(user.getLastName())) {
            user.setLastName(request.lastName());
        }

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public User getAutnenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new DataProcessingException("Unable to find authenticated user");
        }

        return userRepository.findByEmail(authentication.getName()).orElseThrow(
                () -> new EntityNotFoundException("Can't find user by user email: "
                        + authentication.getName()
                )
        );
    }

    @Override
    public boolean existsById(final Long id) {
        return userRepository.existsById(id);
    }

    @Override
    public User getById(final Long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find user by id: " + id)
        );
    }

    @Override
    public User getByEmail(final String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new EntityNotFoundException("Can't find user by email: " + email)
        );
    }

    private User.Role checkValidUserRole(UserUpdateRoleDto updateRoleDto) {
        User.Role role = null;
        String requestRole = updateRoleDto.role().trim().toUpperCase();
        StringBuilder roleMessage = new StringBuilder();

        for (User.Role value : User.Role.values()) {
            roleMessage.append(",").append(" ").append(value.name());

            if (value.name().equals(requestRole)) {
                role = value;
            }
        }

        if (role == null) {
            throw new DataProcessingException("Incorrect role name entered, "
                    + "please enter correct data" + roleMessage.toString());
        }
        return role;
    }
}
