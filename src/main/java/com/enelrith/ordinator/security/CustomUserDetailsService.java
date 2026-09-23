package com.enelrith.ordinator.security;

import com.enelrith.ordinator.user.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException(email));

        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(user.getDeletedAt() != null)
                .build();
    }
}
