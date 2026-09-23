package com.enelrith.ordinator.security;

import com.enelrith.ordinator.common.exception.NotFoundException;
import com.enelrith.ordinator.user.UserMapper;
import com.enelrith.ordinator.user.UserRepository;
import com.enelrith.ordinator.user.dto.UserDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto me(String email) {
        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("User not found"));

        return UserMapper.toUserDto(user);
    }
}
