package com.enelrith.ordinator.user;

import com.enelrith.ordinator.common.exception.AlreadyExistsException;
import com.enelrith.ordinator.user.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_returnsUserDto() {
        var request = new CreateUserRequest(" test@email.com ", "testPassword", " First ", " Last ");

        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.rawPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        var userDto = userService.createUser(request);

        assertEquals("test@email.com", userDto.email());
        assertEquals("First", userDto.firstName());
        assertEquals("Last", userDto.lastName());

        verify(passwordEncoder).encode(request.rawPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_withExistingEmail_throwsAlreadyExistsException() {
        var request = new CreateUserRequest(" test@email.com ", "testPassword", " First ", " Last ");

        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any(User.class));
    }
}
