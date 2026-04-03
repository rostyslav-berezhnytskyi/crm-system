package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.UserEditDto;
import com.els.crmsystem.dto.input.UserInputDto;
import com.els.crmsystem.dto.output.UserOutputDto;
import com.els.crmsystem.entity.User;
import com.els.crmsystem.enums.Role;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Use Spring's Transactional
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service class for managing Users.
 * Handles registration, profile updates, and retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EntityMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    /**
     * Registers a new user in the system.
     * Throws RuntimeException if username or email is already taken.
     */
    @Transactional
    public void registerUser(UserInputDto dto) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("New registration attempt for username: {} by user: {}", dto.username(), adminUser);

        // 1. Validation checks (using getters from DTO record)
        if (userRepository.existsByUsername(dto.username())) {
            throw new RuntimeException("Username is already taken: " + dto.username());
        }
        if (userRepository.existsByEmail(dto.email())) {
            throw new RuntimeException("Email is already registered: " + dto.email());
        }

        // 2. Convert DTO -> Entity using the Mapper
        User user = mapper.toEntity(dto);

        // Hash the password before saving
        String hashedPassword = passwordEncoder.encode(dto.password());
        user.setPassword(hashedPassword);

        userRepository.save(user);

        log.info("SECURITY: Admin '{}' successfully registered user: {}", adminUser, dto.username());
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
    public void adminUpdateUser(Long id, UserEditDto dto, MultipartFile photo) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Admin '{}' updated profile/permissions for User ID: {}", adminUser, id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // 1. Validation for unique username/email
        if (!Objects.equals(user.getUsername(), dto.username()) && userRepository.existsByUsername(dto.username())) {
            throw new RuntimeException("Username is already in use.");
        }
        if (!Objects.equals(user.getEmail(), dto.email()) && userRepository.existsByEmail(dto.email())) {
            throw new RuntimeException("Email is already in use.");
        }

        // 2. Update basic fields
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPhoneNumber(dto.phoneNumber());
        user.setTelegramId(dto.telegramId());
        user.setRole(dto.role());
        user.setEnabled(dto.enabled());

        // 3. Only hash and update password IF the admin typed a new one
        if (dto.newPassword() != null && !dto.newPassword().isBlank()) {
            if (dto.newPassword().length() < 6) {
                throw new RuntimeException("Password must be at least 6 characters.");
            }
            user.setPassword(passwordEncoder.encode(dto.newPassword()));
        }

        // 4. Save Photo into the specific user folder
        if (photo != null && !photo.isEmpty()) {
            String photoPath = fileStorageService.saveFileToSubfolder(photo, "users/" + user.getId());
            user.setPhotoUrl(photoPath);
        }
    }

    /**
     * Deletes a user permanently.
     */
    @Transactional
    public void deleteById(Long id) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        log.warn("CRITICAL: Admin '{}' DELETED User ID: {}", adminUser, id);

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