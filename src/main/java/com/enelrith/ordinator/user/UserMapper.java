package com.enelrith.ordinator.user;

import com.enelrith.ordinator.user.dto.CreateUserRequest;
import com.enelrith.ordinator.user.dto.UserDto;

public class UserMapper {
    private UserMapper() {}

    public static User toEntity(CreateUserRequest request, String passwordHash) {
        return new User(request.email(), passwordHash, request.firstName(), request.lastName());
    }

    public static UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}
