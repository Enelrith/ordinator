package com.enelrith.ordinator.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Size(message = "{user.email.size}", max = 254)
        @Email(message = "{user.email.email}")
        @NotBlank(message = "{user.email.notBlank}") String email,
        @Size(message = "{user.rawPassword.size}", min = 12, max = 64)
        @NotBlank(message = "{user.rawPassword.notBlank}") String rawPassword,
        @Size(message = "{user.firstName.size}", max = 20)
        @Pattern(message = "{user.firstName.pattern}", regexp = "^(?!.*[-']{2})[A-Za-z](?:[A-Za-z'-]*[A-Za-z])?$")
        @NotBlank(message = "{user.firstName.notBlank}") String firstName,
        @Size(message = "{user.lastName.size}", max = 20)
        @Pattern(message = "{user.lastName.pattern}", regexp = "^(?!.*[-']{2})[A-Za-z](?:[A-Za-z'-]*[A-Za-z])?$")
        @NotBlank(message = "{user.lastName.notBlank}") String lastName) {
  public CreateUserRequest {
    email = email == null || email.isBlank() ? null : email.strip();
    firstName = firstName == null || firstName.isBlank() ? null : firstName.strip();
    lastName = lastName == null || lastName.isBlank() ? null : lastName.strip();
  }
}