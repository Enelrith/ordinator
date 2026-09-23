package com.enelrith.ordinator.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(CsrfConfigurer::spa)
                .formLogin(login -> login
                        .usernameParameter("email")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((_, response, _) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                        .failureHandler((_, response, _) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((_, response, _) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((_, response, _) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                        .accessDeniedHandler((_, response, _) ->
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN)
                        )
                )
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                                .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19_456, 2);
    }
}
