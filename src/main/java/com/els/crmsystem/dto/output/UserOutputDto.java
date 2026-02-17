package com.els.crmsystem.dto.output;

import com.els.crmsystem.enums.Role;

/**
 * OUTPUT ONLY: Safe to send to the browser.
 * NO PASSWORD field here.
 */
public record UserOutputDto(
        Long id,
        String username,
        String email,
        String phoneNumber,
        Role role,
        boolean enabled
) {}