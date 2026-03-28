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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${security.remember-me.key}")
    private String rememberMeKey;

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
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC ZONE
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()

                        // 2. GOD MODE (Users)
                        .requestMatchers("/register", "/users", "/users/**").hasRole("ADMIN")
                        .requestMatchers("/").hasAnyRole("ADMIN", "DIRECTOR", "MANAGER") // <-- Блокує доступ Монтажникам до фінансів

                        // 3. MONEY & CRM ZONE (Transactions, Contacts, Companies)
                        .requestMatchers(
                                "/transactions", "/transactions/**",
                                "/contacts", "/contacts/**",
                                "/companies", "/companies/**"
                        ).hasAnyRole("ADMIN", "DIRECTOR", "MANAGER")

                        // 4. PROJECT CREATION & MANAGEMENT (Суворо для Керівництва)
                        .requestMatchers(HttpMethod.POST, "/projects").hasAnyRole("ADMIN", "DIRECTOR", "MANAGER") // <-- Закриває дірку з POST
                        .requestMatchers(
                                "/projects/new",
                                "/projects/edit/**",
                                "/projects/update/**",
                                "/projects/delete/**",
                                "/projects/*/equipment",
                                "/projects/equipment/*/delete",
                                "/projects/*/contacts/add",
                                "/projects/*/contacts/*/remove"
                        ).hasAnyRole("ADMIN", "DIRECTOR", "MANAGER")

                        // 5. VIEWING THE PROJECT LIST
                        // (Guests/Clients are allowed to see the main list if you want them to)
                        .requestMatchers(HttpMethod.GET, "/projects").hasAnyRole("ADMIN", "DIRECTOR", "MANAGER", "INSTALLER", "GUEST")

                        // 6. OPERATIONAL ZONE (View Projects, Upload Photos)
                        // INSTALLER is allowed here.
                        .requestMatchers("/projects/**", "/uploads/**")
                        .hasAnyRole("ADMIN", "DIRECTOR", "MANAGER", "INSTALLER")

                        // 7. CATCH-ALL FOR GUESTS
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                // --- REMEMBER ME CONFIGURATION ---
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(86400) // 1 day
                        .userDetailsService(userDetailsService)
                        .alwaysRemember(true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("remember-me")
                        .permitAll()
                );

        return http.build();
    }

    // 3. Connect UserLoader to Security
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);

        // Line 2: Sets the Password Encryptor
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
