package com.enelrith.ordinator.user;

import com.enelrith.ordinator.common.exception.AlreadyExistsException;
import com.enelrith.ordinator.user.dto.CreateUserRequest;
import com.enelrith.ordinator.user.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) throw new AlreadyExistsException("This email is already in use");

        var passwordHash = passwordEncoder.encode(request.rawPassword());
        var user = UserMapper.toEntity(request, passwordHash);
        userRepository.save(user);

        log.info("Created user {}", user.getId());

        return UserMapper.toUserDto(user);
    }
}
