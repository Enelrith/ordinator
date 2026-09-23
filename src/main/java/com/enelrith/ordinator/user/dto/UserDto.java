package com.enelrith.ordinator.user.dto;

import java.util.UUID;

public record UserDto(UUID id, String email, String firstName, String lastName) {
}