package com.els.crmsystem.config;

import com.els.crmsystem.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${security.remember-me.key}")
    private String rememberMeKey;

    private final CustomUserDetailsService userDetailsService;
    private final DataSource dataSource; // <-- Needed for secure Remember-Me

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // High-Security Persistent Token Repository
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC ZONE
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()

                        // 2. GOD MODE (Users)
                        .requestMatchers("/register", "/users", "/users/**").hasRole("ADMIN")

                        // 3. PIPELINE, MONEY & CRM ZONE (Management Only)
                        .requestMatchers(
                                "/", "/pipeline",
                                "/transactions", "/transactions/**",
                                "/contacts", "/contacts/**",
                                "/companies", "/companies/**"
                        ).hasAnyRole("ADMIN", "DIRECTOR", "MANAGER")

                        // 4. PROJECT MANAGEMENT (Write Access - Management Only)
                        .requestMatchers(HttpMethod.POST, "/projects", "/projects/**").hasAnyRole("ADMIN", "DIRECTOR", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/projects/**").hasAnyRole("ADMIN", "DIRECTOR", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/projects/**").hasAnyRole("ADMIN", "DIRECTOR", "MANAGER")
                        .requestMatchers(
                                "/projects/new", "/projects/edit/**"
                        ).hasAnyRole("ADMIN", "DIRECTOR", "MANAGER")

                        // 5. CALENDAR & TASKS (Management + Installers)
                        .requestMatchers(
                                "/tasks", "/tasks/**", "/api/tasks/**",
                                "/calendar", "/calendar/**", "/api/calendar/**"
                        ).hasAnyRole("ADMIN", "DIRECTOR", "MANAGER", "INSTALLER")

                        // 6. OPERATIONAL ZONE (Read Access & File Uploads - Management + Installers + Guests)
                        .requestMatchers(HttpMethod.GET, "/projects").hasAnyRole("ADMIN", "DIRECTOR", "MANAGER", "INSTALLER", "GUEST")
                        .requestMatchers("/projects/**", "/uploads/**").hasAnyRole("ADMIN", "DIRECTOR", "MANAGER", "INSTALLER")

                        // 7. CATCH-ALL
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false) // <-- FIXED: Safer fallback redirect
                        .permitAll()
                )
                .rememberMe(remember -> remember
                                .key(rememberMeKey)
                                .tokenValiditySeconds(86400) // 1 day
                                .tokenRepository(persistentTokenRepository()) // <-- FIXED: Uses database instead of stateless cookie
                                .userDetailsService(userDetailsService)
                        // .alwaysRemember(true) <-- DELETED: As requested by the audit
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("remember-me")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}