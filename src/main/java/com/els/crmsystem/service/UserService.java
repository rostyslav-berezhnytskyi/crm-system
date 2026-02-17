package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.UserInputDto;
import com.els.crmsystem.dto.output.UserOutputDto;
import com.els.crmsystem.entity.User;
import com.els.crmsystem.enums.Role;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Use Spring's Transactional

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service class for managing Users.
 * Handles registration, profile updates, and retrieval.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EntityMapper mapper;

    /**
     * Registers a new user in the system.
     * Throws RuntimeException if username or email is already taken.
     */
    @Transactional
    public void registerUser(UserInputDto dto) {
        // 1. Validation checks (using getters from DTO record)
        if (userRepository.existsByUsername(dto.username())) {
            throw new RuntimeException("Username is already taken: " + dto.username());
        }
        if (userRepository.existsByEmail(dto.email())) {
            throw new RuntimeException("Email is already registered: " + dto.email());
        }

        // 2. Convert DTO -> Entity using the Mapper
        User user = mapper.toEntity(dto);

        // 3. Any logic NOT handled by the mapper?
        // Our mapper sets default Role.CLIENT and Enabled=true, so we are good.
        // Later, we will add: user.setPassword(passwordEncoder.encode(dto.password()));

        userRepository.save(user);
    }

    /**
     * Finds a user by ID and converts it to a safe DTO.
     */
    public UserOutputDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return mapper.toOutputDto(user);
    }

    /**
     * Finds a user by Username and converts it to a safe DTO.
     */
    public UserOutputDto findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return mapper.toOutputDto(user);
    }

    /**
     * Updates an existing user's profile.
     * Includes logic to prevent taking someone else's username/email.
     */
    @Transactional
    public void updateUser(Long id, UserInputDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // Validation
        if (!Objects.equals(user.getUsername(), dto.username()) && userRepository.existsByUsername(dto.username())) {
            throw new RuntimeException("Username '" + dto.username() + "' is already in use.");
        }

        if (!Objects.equals(user.getEmail(), dto.email()) && userRepository.existsByEmail(dto.email())) {
            throw new RuntimeException("Email '" + dto.email() + "' is already in use.");
        }

        // Manual update is often safer than mapper here,
        // because we don't want to accidentally overwrite ID or Password if the DTO is partial.
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPhoneNumber(dto.phoneNumber());
        // Note: Password update usually requires a separate specific method for security.
    }

    /**
     * Deletes a user permanently.
     */
    @Transactional
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. User not found: " + id);
        }
        userRepository.deleteById(id);
    }


    /**
     * Returns a list of all users in the system as DTOs.
     */
    public List<UserOutputDto> findAll() {
        return userRepository.findAll().stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

}