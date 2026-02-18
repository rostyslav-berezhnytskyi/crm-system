package com.els.crmsystem.config;

import com.els.crmsystem.security.CustomUserDetailsService;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    // 1. The Password Encoder Bean (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. The Filter Chain (The Rules)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Define URL rules
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC ZONE (Login, CSS, JS, Images)
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()

                        // 2. RESTRICTED ZONE (Everything else)
                        // ONLY users with role 'ADMIN' can enter these pages.
                        // A 'CLIENT' (hacker) will be blocked here.
                        .requestMatchers("/register", "/users/**").hasRole("ADMIN")
                        .requestMatchers("/projects/**").hasAnyRole("ADMIN", "DIRECTOR")
                        .requestMatchers("/transactions/**").hasAnyRole("ADMIN", "DIRECTOR")
                        .requestMatchers("/uploads/**").hasAnyRole("ADMIN", "DIRECTOR")

                        // 3. CATCH-ALL
                        // If we forgot a URL, default to requiring login (safe backup)
                        .anyRequest().authenticated()
                )
                // Define Login Page
                .formLogin(form -> form
                        .loginPage("/login") // We will create this HTML file
                        .defaultSuccessUrl("/transactions", true) // Where to go after login
                        .permitAll()
                )
                // Define Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    // 3. Connect UserLoader to Security
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);

        // Line 2: Sets the Password Encryptor
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
