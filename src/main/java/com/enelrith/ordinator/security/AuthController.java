package com.enelrith.ordinator.security;

import com.enelrith.ordinator.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(description = "Provides a CSRF token for subsequent requests")
    @ApiResponse(responseCode = "204", description = "CSRF token provided")
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    @Operation(description = "Returns the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Authenticated user returned")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        var userDto = authService.me(authentication.getName());

        return ResponseEntity.ok(userDto);
    }
}
