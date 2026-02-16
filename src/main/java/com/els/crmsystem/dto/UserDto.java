package com.els.crmsystem.dto;

import com.els.crmsystem.enums.Role;

public record UserDto(
        Long id,
        String username,
        String email,
        Role role
) {}
