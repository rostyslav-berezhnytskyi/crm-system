package com.els.crmsystem.security;

import com.els.crmsystem.entity.User;
import com.els.crmsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventListener {

    private final UserRepository userRepository;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @EventListener
    @Transactional
    public void authenticationFailed(AuthenticationFailureBadCredentialsEvent event) {
        String username = (String) event.getAuthentication().getPrincipal();
        userRepository.findByUsername(username).ifPresent(user -> {
            if (!user.isLocked()) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                log.warn("Failed login attempt {} for user {}", user.getFailedLoginAttempts(), username);

                if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                    user.setLocked(true);
                    log.error("USER LOCKED due to brute force: {}", username);
                }
                userRepository.save(user);
            }
        });
    }

    @EventListener
    @Transactional
    public void authenticationSucceeded(AuthenticationSuccessEvent event) {
        // Reset the counter when they successfully log in!
        String username = ((org.springframework.security.core.userdetails.User) event.getAuthentication().getPrincipal()).getUsername();
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }
        });
    }
}
