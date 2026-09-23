package com.enelrith.ordinator.user;

import com.enelrith.ordinator.user.dto.CreateUserRequest;
import com.enelrith.ordinator.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(description = "Creates a new user in the database")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Invalid content in the request body")
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token")
    @ApiResponse(responseCode = "409", description = "Email is already in use by another user")
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        var userDto = userService.createUser(request);
        var uri = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .pathSegment(userDto.id().toString())
                .build()
                .toUri();
        return ResponseEntity.created(uri).body(userDto);
    }
}
