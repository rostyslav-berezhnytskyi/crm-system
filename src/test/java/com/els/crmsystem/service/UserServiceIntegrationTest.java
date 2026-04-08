package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.UserEditDto;
import com.els.crmsystem.dto.input.UserInputDto;
import com.els.crmsystem.dto.output.UserOutputDto;
import com.els.crmsystem.entity.User;
import com.els.crmsystem.enums.Role;
import com.els.crmsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for UserService.
 *
 * Strategy:
 * - Full Spring context against H2 in-memory DB (configured in src/test/resources/application.yaml).
 * - Flyway disabled; schema created via Hibernate create-drop.
 * - @MockitoBean for AuditNotificationService (Telegram HTTP) and FileStorageService (disk I/O).
 * - @Transactional at class level: each test rolls back automatically → clean slate per test.
 * - @Autowired PasswordEncoder (from SecurityConfig) is used to assert password hashing
 *   without relying on implementation details of the BCrypt output format.
 *
 * Primary focus: uniqueness enforcement, password hashing, and the edge cases
 * that silently corrupt data or block legitimate users if broken.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@DisplayName("UserService — Integration Tests")
class UserServiceIntegrationTest {

    @Autowired private UserService    userService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;   // BCryptPasswordEncoder from SecurityConfig

    @MockitoBean private AuditNotificationService auditNotificationService;
    @MockitoBean private FileStorageService       fileStorageService;

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Creates and saves a user directly via the repository (bypasses the service). */
    private User seedUser(String username, String email, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("seed-password-123"));
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private UserInputDto registerDto(String username, String password, String email) {
        return new UserInputDto(username, password, email, null);
    }

    private UserEditDto editDto(String username, String email, Role role,
                                boolean enabled, String newPassword) {
        return new UserEditDto(username, email, "+380 50 000 0001", null, role, enabled, newPassword);
    }

    // ===========================================================================
    // registerUser
    // ===========================================================================
    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("valid registration: user is persisted and findable by username")
        void validRegistration_userPersistedToDatabase() {
            userService.registerUser(registerDto("alice", "strongPass", "alice@test.com"));

            assertThat(userRepository.existsByUsername("alice")).isTrue();
        }

        @Test
        @DisplayName("valid registration: password stored in DB is NOT the plaintext value")
        void validRegistration_passwordIsNotStoredAsPlaintext() {
            userService.registerUser(registerDto("bob", "plaintext99", "bob@test.com"));

            User saved = userRepository.findByUsername("bob").orElseThrow();
            assertThat(saved.getPassword()).isNotEqualTo("plaintext99");
        }

        @Test
        @DisplayName("valid registration: stored hash verifies correctly with BCrypt")
        void validRegistration_storedHashVerifiesWithBCrypt() {
            userService.registerUser(registerDto("carol", "mySecret42", "carol@test.com"));

            User saved = userRepository.findByUsername("carol").orElseThrow();
            assertThat(passwordEncoder.matches("mySecret42", saved.getPassword())).isTrue();
        }

        @Test
        @DisplayName("valid registration: wrong password does NOT match the stored hash")
        void validRegistration_wrongPasswordDoesNotMatch() {
            userService.registerUser(registerDto("dave", "correctPass", "dave@test.com"));

            User saved = userRepository.findByUsername("dave").orElseThrow();
            assertThat(passwordEncoder.matches("wrongPass", saved.getPassword())).isFalse();
        }

        @Test
        @DisplayName("valid registration: default role is GUEST")
        void validRegistration_defaultRoleIsGuest() {
            userService.registerUser(registerDto("eve", "password123", "eve@test.com"));

            User saved = userRepository.findByUsername("eve").orElseThrow();
            assertThat(saved.getRole()).isEqualTo(Role.GUEST);
        }

        @Test
        @DisplayName("valid registration: user is enabled by default")
        void validRegistration_enabledByDefault() {
            userService.registerUser(registerDto("frank", "password123", "frank@test.com"));

            User saved = userRepository.findByUsername("frank").orElseThrow();
            assertThat(saved.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("duplicate username → throws RuntimeException mentioning the taken username")
        void duplicateUsername_throwsRuntimeException() {
            seedUser("grace", "grace@test.com", Role.MANAGER);

            assertThatThrownBy(() ->
                    userService.registerUser(registerDto("grace", "newPass123", "other@test.com")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("grace");
        }

        @Test
        @DisplayName("duplicate email → throws RuntimeException mentioning 'already registered'")
        void duplicateEmail_throwsRuntimeException() {
            seedUser("heidi", "shared@test.com", Role.MANAGER);

            assertThatThrownBy(() ->
                    userService.registerUser(registerDto("newuser", "newPass123", "shared@test.com")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("duplicate username does NOT save the second user to the database")
        void duplicateUsername_secondUserNotPersisted() {
            seedUser("ivan", "ivan@test.com", Role.MANAGER);

            try {
                userService.registerUser(registerDto("ivan", "password123", "other@test.com"));
            } catch (RuntimeException ignored) {}

            // Only the seeded user should exist, not a duplicate
            assertThat(userRepository.findAll()).hasSize(1);
        }
    }

    // ===========================================================================
    // adminUpdateUser
    // ===========================================================================
    @Nested
    @DisplayName("adminUpdateUser")
    class AdminUpdateUser {

        @Test
        @DisplayName("updates all basic fields: username, email, phone, role, enabled")
        void updatesAllBasicFields() {
            User user = seedUser("judy", "judy@test.com", Role.GUEST);
            UserEditDto dto = new UserEditDto(
                    "judy-updated", "judy-new@test.com",
                    "+380 67 111 2233", "tg-123",
                    Role.MANAGER, false, null);

            userService.adminUpdateUser(user.getId(), dto, null);

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getUsername()).isEqualTo("judy-updated");
            assertThat(updated.getEmail()).isEqualTo("judy-new@test.com");
            assertThat(updated.getPhoneNumber()).isEqualTo("+380 67 111 2233");
            assertThat(updated.getTelegramId()).isEqualTo("tg-123");
            assertThat(updated.getRole()).isEqualTo(Role.MANAGER);
            assertThat(updated.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("providing a new password → it is stored as a BCrypt hash, not plaintext")
        void newPassword_isHashed() {
            User user = seedUser("kate", "kate@test.com", Role.MANAGER);
            UserEditDto dto = editDto("kate", "kate@test.com", Role.MANAGER, true, "brand-new-pass");

            userService.adminUpdateUser(user.getId(), dto, null);

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getPassword()).isNotEqualTo("brand-new-pass");
        }

        @Test
        @DisplayName("providing a new password → stored hash is verifiable with BCrypt")
        void newPassword_verifiableWithBCrypt() {
            User user = seedUser("leo", "leo@test.com", Role.INSTALLER);
            UserEditDto dto = editDto("leo", "leo@test.com", Role.INSTALLER, true, "updated-secret");

            userService.adminUpdateUser(user.getId(), dto, null);

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(passwordEncoder.matches("updated-secret", updated.getPassword())).isTrue();
        }

        @Test
        @DisplayName("null newPassword → existing password hash is preserved unchanged")
        void nullNewPassword_existingPasswordPreserved() {
            User user = seedUser("mia", "mia@test.com", Role.MANAGER);
            String originalHash = user.getPassword();
            UserEditDto dto = editDto("mia", "mia@test.com", Role.MANAGER, true, null);

            userService.adminUpdateUser(user.getId(), dto, null);

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getPassword()).isEqualTo(originalHash);
        }

        @Test
        @DisplayName("blank newPassword → existing password hash is preserved unchanged")
        void blankNewPassword_existingPasswordPreserved() {
            User user = seedUser("nina", "nina@test.com", Role.MANAGER);
            String originalHash = user.getPassword();
            UserEditDto dto = editDto("nina", "nina@test.com", Role.MANAGER, true, "   ");

            userService.adminUpdateUser(user.getId(), dto, null);

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getPassword()).isEqualTo(originalHash);
        }

        @Test
        @DisplayName("newPassword shorter than 6 characters → throws RuntimeException")
        void shortNewPassword_throwsRuntimeException() {
            User user = seedUser("oscar", "oscar@test.com", Role.GUEST);
            UserEditDto dto = editDto("oscar", "oscar@test.com", Role.GUEST, true, "abc");

            assertThatThrownBy(() -> userService.adminUpdateUser(user.getId(), dto, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("6");
        }

        @Test
        @DisplayName("newPassword of exactly 5 characters → throws (boundary: < 6 fails)")
        void exactlyFiveCharPassword_throwsRuntimeException() {
            User user = seedUser("pete", "pete@test.com", Role.GUEST);
            UserEditDto dto = editDto("pete", "pete@test.com", Role.GUEST, true, "12345");

            assertThatThrownBy(() -> userService.adminUpdateUser(user.getId(), dto, null))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("newPassword of exactly 6 characters → accepted (boundary: >= 6 passes)")
        void exactlySixCharPassword_isAccepted() {
            User user = seedUser("quinn", "quinn@test.com", Role.GUEST);
            UserEditDto dto = editDto("quinn", "quinn@test.com", Role.GUEST, true, "123456");

            userService.adminUpdateUser(user.getId(), dto, null);   // must not throw

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(passwordEncoder.matches("123456", updated.getPassword())).isTrue();
        }

        @Test
        @DisplayName("changing to another user's existing username → throws RuntimeException")
        void changingToAnotherUsersUsername_throws() {
            seedUser("alice", "alice@test.com", Role.MANAGER);
            User bob = seedUser("bob", "bob@test.com", Role.MANAGER);

            UserEditDto dto = editDto("alice", "bob@test.com", Role.MANAGER, true, null);

            assertThatThrownBy(() -> userService.adminUpdateUser(bob.getId(), dto, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already in use");
        }

        @Test
        @DisplayName("changing to another user's existing email → throws RuntimeException")
        void changingToAnotherUsersEmail_throws() {
            seedUser("alice", "alice@test.com", Role.MANAGER);
            User bob = seedUser("bob", "bob@test.com", Role.MANAGER);

            UserEditDto dto = editDto("bob", "alice@test.com", Role.MANAGER, true, null);

            assertThatThrownBy(() -> userService.adminUpdateUser(bob.getId(), dto, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already in use");
        }

        @Test
        @DisplayName("keeping own username → not treated as a conflict, no exception thrown")
        void keepingOwnUsername_doesNotThrow() {
            User user = seedUser("sam", "sam@test.com", Role.MANAGER);

            // Updating to the same username — must NOT throw even though it "exists"
            UserEditDto dto = editDto("sam", "sam-new@test.com", Role.MANAGER, true, null);

            userService.adminUpdateUser(user.getId(), dto, null);   // must not throw

            assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail())
                    .isEqualTo("sam-new@test.com");
        }

        @Test
        @DisplayName("keeping own email → not treated as a conflict, no exception thrown")
        void keepingOwnEmail_doesNotThrow() {
            User user = seedUser("tina", "tina@test.com", Role.MANAGER);

            // Updating to the same email — must NOT throw even though it "exists"
            UserEditDto dto = editDto("tina-new", "tina@test.com", Role.MANAGER, true, null);

            userService.adminUpdateUser(user.getId(), dto, null);   // must not throw

            assertThat(userRepository.findById(user.getId()).orElseThrow().getUsername())
                    .isEqualTo("tina-new");
        }

        @Test
        @DisplayName("disabling a user → enabled=false is persisted")
        void disablingUser_persistsEnabledFalse() {
            User user = seedUser("uma", "uma@test.com", Role.INSTALLER);
            assertThat(user.isEnabled()).isTrue();

            UserEditDto dto = editDto("uma", "uma@test.com", Role.INSTALLER, false, null);
            userService.adminUpdateUser(user.getId(), dto, null);

            assertThat(userRepository.findById(user.getId()).orElseThrow().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("non-existent user id → throws RuntimeException with 'User not found'")
        void nonExistentId_throwsRuntimeException() {
            UserEditDto dto = editDto("nobody", "nobody@test.com", Role.GUEST, true, null);

            assertThatThrownBy(() -> userService.adminUpdateUser(999_999L, dto, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ===========================================================================
    // deleteById
    // ===========================================================================
    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("existing user → removed from the database")
        void existingUser_deletedFromDatabase() {
            User user = seedUser("vera", "vera@test.com", Role.GUEST);
            assertThat(userRepository.findById(user.getId())).isPresent();

            userService.deleteById(user.getId());

            assertThat(userRepository.findById(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("deleting one user → sibling users remain untouched")
        void deletingOneUser_othersUntouched() {
            User target = seedUser("walter", "walter@test.com", Role.INSTALLER);
            User keeper = seedUser("xena",   "xena@test.com",   Role.MANAGER);

            userService.deleteById(target.getId());

            assertThat(userRepository.findById(keeper.getId())).isPresent();
        }

        @Test
        @DisplayName("non-existent id → throws RuntimeException with 'Cannot delete'")
        void nonExistentId_throwsRuntimeException() {
            assertThatThrownBy(() -> userService.deleteById(999_999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot delete");
        }
    }

    // ===========================================================================
    // findById
    // ===========================================================================
    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("existing user → returns UserOutputDto with correct field values")
        void existingUser_returnsCorrectDto() {
            User user = seedUser("yara", "yara@test.com", Role.DIRECTOR);

            UserOutputDto dto = userService.findById(user.getId());

            assertThat(dto.id()).isEqualTo(user.getId());
            assertThat(dto.username()).isEqualTo("yara");
            assertThat(dto.email()).isEqualTo("yara@test.com");
            assertThat(dto.role()).isEqualTo(Role.DIRECTOR);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("UserOutputDto does NOT expose a password field")
        void returnedDtoHasNoPassword() {
            User user = seedUser("zack", "zack@test.com", Role.GUEST);

            UserOutputDto dto = userService.findById(user.getId());

            // Compile-time guarantee: UserOutputDto has no password() accessor.
            // We verify none of the string fields contains the hash.
            String hash = userRepository.findById(user.getId()).orElseThrow().getPassword();
            assertThat(dto.username()).doesNotContain(hash);
            assertThat(dto.email()).doesNotContain(hash);
        }

        @Test
        @DisplayName("non-existent id → throws RuntimeException with 'User not found'")
        void nonExistentId_throwsRuntimeException() {
            assertThatThrownBy(() -> userService.findById(999_999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ===========================================================================
    // findByUsername
    // ===========================================================================
    @Nested
    @DisplayName("findByUsername")
    class FindByUsername {

        @Test
        @DisplayName("existing username → returns UserOutputDto with matching username")
        void existingUsername_returnsCorrectDto() {
            seedUser("ada", "ada@test.com", Role.ADMIN);

            UserOutputDto dto = userService.findByUsername("ada");

            assertThat(dto.username()).isEqualTo("ada");
            assertThat(dto.email()).isEqualTo("ada@test.com");
            assertThat(dto.role()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("non-existent username → throws RuntimeException with 'User not found'")
        void nonExistentUsername_throwsRuntimeException() {
            assertThatThrownBy(() -> userService.findByUsername("ghost"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ===========================================================================
    // findAll
    // ===========================================================================
    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("empty database → returns empty list")
        void emptyDatabase_returnsEmptyList() {
            List<UserOutputDto> result = userService.findAll();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns one DTO per user in the database")
        void returnsOneDtoPerUser() {
            seedUser("user1", "user1@test.com", Role.MANAGER);
            seedUser("user2", "user2@test.com", Role.INSTALLER);
            seedUser("user3", "user3@test.com", Role.GUEST);

            List<UserOutputDto> result = userService.findAll();

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("returned DTOs contain the correct usernames")
        void returnedDtosHaveCorrectUsernames() {
            seedUser("alpha", "alpha@test.com", Role.MANAGER);
            seedUser("beta",  "beta@test.com",  Role.GUEST);

            List<String> usernames = userService.findAll().stream()
                    .map(UserOutputDto::username)
                    .toList();

            assertThat(usernames).containsExactlyInAnyOrder("alpha", "beta");
        }
    }
}