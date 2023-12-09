package booking.app.mapper;

import booking.app.config.MapperConfig;
import booking.app.dto.user.UserRegisterRequestDto;
import booking.app.dto.user.UserResponseDto;
import booking.app.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    UserResponseDto toDto(User user);

    @Mapping(target = "role", expression = "java(User.Role.CUSTOMER)")
    User toEntity(UserRegisterRequestDto request);
}
