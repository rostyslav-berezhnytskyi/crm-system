package com.els.crmsystem.service;

import com.els.crmsystem.dto.UserDto;
import com.els.crmsystem.entity.User;
import com.els.crmsystem.enums.Role;
import com.els.crmsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Use Spring's Transactional

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void registerUser(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered: " + email);
        }

        User user = new User();
        user.setUsername(username);

        // TODO: SECURITY RISK! We must hash this password later using BCrypt.
        // For now (Development only), plain text is allowed but dangerous.
        user.setPassword(password);

        user.setEmail(email);
        user.setRole(Role.CLIENT); // Default role is safe

        userRepository.save(user);
    }

    public UserDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return mapToDto(user);
    }

    public UserDto findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return mapToDto(user);
    }

    @Transactional
    public void updateUser(Long id, String username, String email, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // TODO: Check if new username/email is already taken by ANOTHER user
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        // No need to call .save() here because @Transactional does "Dirty Checking"
    }

    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    // Internal Helper to convert Entity -> DTO
    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }

    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
}