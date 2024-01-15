package booking.app.service.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Register user by exists email, should return UserResponseDto")
    void registerUser_ByNonExistsEmail_ShouldReturnUser() {
        //Given
        UserRegisterRequestDto requestDto = createRegisterRequest();
        User user = createUserFromRequest(requestDto);
        UserResponseDto expected = new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );

        //When
        when(userRepository.existsByEmail(requestDto.getEmail())).thenReturn(false);
        when(userMapper.toEntity(requestDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        //Then
        try {
            UserResponseDto actual = userService.register(requestDto);
            assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

            verify(userRepository, times(1)).existsByEmail(requestDto.getEmail());
            verify(userMapper, times(1)).toEntity(requestDto);
            verify(userRepository, times(1)).save(user);
            verify(userMapper, times(1)).toDto(user);
        } catch (RegistrationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Register user by exists email, should return exception")
    void registerUser_ByExistsEmail_ShouldReturnException() {
        //Given
        UserRegisterRequestDto requestDto = createRegisterRequest();
        User user = createUserFromRequest(requestDto);

        //When
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
        Exception exception = assertThrows(
                RegistrationException.class,
                () -> userService.register(requestDto)
        );

        //Then
        String expected = "Can't complete registration";
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).existsByEmail(user.getEmail());
    }

    @Test
    @DisplayName("Get user profile with authenticated user, should return UserResponseDto")
    void getUserProfile_WithAuthenticatedUser_ShouldReturnUser() {
        // Given
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = createUserFromRequest(createRegisterRequest());
        UserResponseDto expected = new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );

        // When
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(
                authentication.getName())).thenReturn(Optional.of(user)
        );
        when(userMapper.toDto(user)).thenReturn(expected);

        // Then
        UserResponseDto actual = userService.getUserProfile();
        assertTrue(EqualsBuilder.reflectionEquals(actual, expected));
        verify(userRepository, times(1)).findByEmail(authentication.getName());
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    @DisplayName("Get user profile with unauthenticated user, should return exception")
    void getUserProfile_WithUnauthenticatedUser_ShouldReturnException() {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);

        //When
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> userService.getUserProfile()
        );

        //Then
        String expected = "Unable to find authenticated user";
        String actual = exception.getMessage();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Get user profile with authenticated user but non "
            + "exists from db, should return exception")
    void getUserProfile_WithAuthenticatedUserButNotFromDb_ShouldReturnException() {
        // Given
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = createUserFromRequest(createRegisterRequest());

        // When
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(
                authentication.getName())).thenReturn(Optional.empty()
        );
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.getUserProfile()
        );

        // Then
        String expected = "Can't find user by user email: " + authentication.getName();
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).findByEmail(authentication.getName());
    }

    @Test
    @DisplayName("Update user role by valid id and valid role, dont throw exception")
    void updateUserRole_ByValidIdAndValidRole_DontThrowException() {
        //Given
        final User user = createUserFromRequest(createRegisterRequest());
        user.setRole(User.Role.ROLE_MANAGER);
        UserUpdateRoleDto updateRoleDto = new UserUpdateRoleDto("ROLE_CUSTOMER");

        //When
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        //Then
        assertDoesNotThrow(() -> userService.updateUserRole(user.getId(), updateRoleDto));
        verify(userRepository, times(1)).findById(user.getId());
    }

    @Test
    @DisplayName("Update user role by invalid user id, should return exception")
    void updateUserRole_ByInvalidUserId_ShouldReturnException() {
        //Given
        Long id = 999L;
        UserUpdateRoleDto updateRoleDto = new UserUpdateRoleDto("ROLE_CUSTOMER");

        //When
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.updateUserRole(id, updateRoleDto)
        );

        //Then
        String expected = "Can't find user by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Update user role by invalid role name, should return exception")
    void updateUserRole_ByInvalidRoleName_ShouldReturnException() {
        //Given
        User user = createUserFromRequest(createRegisterRequest());
        UserUpdateRoleDto updateRoleDto = new UserUpdateRoleDto("ADMIN");
        StringBuilder expected = new StringBuilder(
                "Incorrect role name entered, please enter correct data"
        );

        for (User.Role role : User.Role.values()) {
            expected.append(",").append(" ").append(role.name());
        }

        //When
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> userService.updateUserRole(user.getId(), updateRoleDto)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected.toString(), actual);
        verify(userRepository, times(1)).findById(user.getId());
    }

    @Test
    @DisplayName("Update user profile by valid data, should return UserResponseDto")
    void updateUserProfile_ByValidData_ShouldReturnUserDto() {
        //Given
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = createUserFromRequest(createRegisterRequest());
        UserUpdateProfileInformationDto requestUpdate = new UserUpdateProfileInformationDto(
                "update@example.com",
                "Update=123456789",
                "Update",
                "Update"
        );
        UserResponseDto expected = new UserResponseDto(
                user.getId(),
                requestUpdate.email(),
                requestUpdate.firstName(),
                requestUpdate.lastName()
        );

        //When
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(
                authentication.getName())).thenReturn(Optional.of(user)
        );
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        //Then
        UserResponseDto actual = userService.updateUserProfile(requestUpdate);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(userRepository, times(1)).findByEmail(authentication.getName());
        verify(userRepository, times(1)).save(user);
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    @DisplayName("Update user profile by null value some fields, should return UserResponseDto "
            + "with update valid fields")
    void updateUserProfile_ByNullValueSomeFields_ShouldReturnUserDtoWithUpdatedValidFields() {
        //Given
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = createUserFromRequest(createRegisterRequest());
        UserUpdateProfileInformationDto requestUpdate = new UserUpdateProfileInformationDto(
                null,
                "Update=123456789",
                "Update",
                "Update"
        );
        UserResponseDto expected = new UserResponseDto(
                user.getId(),
                user.getEmail(),
                requestUpdate.firstName(),
                requestUpdate.lastName()
        );

        //When
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(
                authentication.getName())).thenReturn(Optional.of(user)
        );
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        //Then
        UserResponseDto actual = userService.updateUserProfile(requestUpdate);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(userRepository, times(1)).findByEmail(authentication.getName());
        verify(userRepository, times(1)).save(user);
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    @DisplayName("Update user profile with all null fields, should return UserResponseDto "
            + "without updated")
    void updateUserProfile_WithAllNullField_ShouldReturnUserDtoWithoutUpdated() {
        //Given
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = createUserFromRequest(createRegisterRequest());
        UserUpdateProfileInformationDto requestUpdate = new UserUpdateProfileInformationDto(
                null,
                null,
                null,
                null
        );

        UserResponseDto expected = new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );

        //When
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(
                authentication.getName())).thenReturn(Optional.of(user)
        );
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        //Then
        UserResponseDto actual = userService.updateUserProfile(requestUpdate);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(userRepository, times(1)).findByEmail(authentication.getName());
        verify(userRepository, times(1)).save(user);
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    @DisplayName("Update user profile with unauthenticated user, should return exception")
    void updateUserProfile_WithUnauthenticatedUser_ShouldReturnException() {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);
        UserUpdateProfileInformationDto requestUpdate = new UserUpdateProfileInformationDto(
                "update@example.com",
                "Update=123456789",
                "Update",
                "Update"
        );

        //When
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> userService.updateUserProfile(requestUpdate)
        );

        //Then
        String expected = "Unable to find authenticated user";
        String actual = exception.getMessage();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Exists by id by valid id, should return true")
    void existsById_ByValidId_ShouldReturnTrue() {
        //Given
        Long id = 1L;

        //When
        when(userRepository.existsById(id)).thenReturn(true);

        //Then
        assertTrue(userService.existsById(id));
        verify(userRepository, times(1)).existsById(id);
    }

    @Test
    @DisplayName("Exists by ud by invalid id, should return false")
    void existsById_ByInvalidId_ShouldReturnFalse() {
        //Given
        Long id = 999L;

        //When
        when(userRepository.existsById(id)).thenReturn(false);

        //Then
        assertFalse(userService.existsById(id));
        verify(userRepository, times(1)).existsById(id);
    }

    @Test
    @DisplayName("Get by id by valid id, should return user")
    void getById_ByValidId_ShouldReturnUser() {
        //Given
        Long id = 1L;
        User expected = createUserFromRequest(createRegisterRequest());

        //When
        when(userRepository.findById(id)).thenReturn(Optional.of(expected));

        //Then
        User actual = userService.getById(id);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get by id by invalid id, should return exception")
    void getById_ByInvalidId_ShouldReturnException() {
        //Given
        Long id = 999L;

        //When
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.getById(id)
        );

        //Then
        String expected = "Can't find user by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get by email by valid email, should return user")
    void getByEmail_ByValidEmail_ShouldReturnUser() {
        //Given
        String email = "customer@example.com";
        User expected = createUserFromRequest(createRegisterRequest());

        //When
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(expected));

        //Then
        User actual = userService.getByEmail(email);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Get by email by invalid email, should return exception")
    void getByEmail_ByInvalidEmail_ShouldReturnException() {
        //Given
        String email = "cus@example.com";

        //When
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.getByEmail(email)
        );

        //Then
        String expected = "Can't find user by email: " + email;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).findByEmail(email);
    }

    private UserRegisterRequestDto createRegisterRequest() {
        UserRegisterRequestDto requestDto = new UserRegisterRequestDto();
        requestDto.setEmail("customer@example.com");
        requestDto.setPassword("User=123456789");
        requestDto.setRepeatPassword("User=123456789");
        requestDto.setFirstName("Customer");
        requestDto.setLastName("Customer");
        return requestDto;
    }

    private User createUserFromRequest(UserRegisterRequestDto request) {
        User user = new User();
        user.setId(1L);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(User.Role.ROLE_CUSTOMER);
        return user;
    }
}
