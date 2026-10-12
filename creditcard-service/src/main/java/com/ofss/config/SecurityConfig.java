package com.ofss.config;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${CARD_ADMIN_USERNAME}") String username,
            @Value("${CARD_ADMIN_PASSWORD}") String password) {

        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                "Admin username and password must not be blank"
            );
        }

        return new InMemoryUserDetailsManager(
            User.withUsername(username)
                .password(encoder.encode(password))
                .roles("ADMIN")
                .build()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // These API requests use credentials, not browser sessions.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/cards", "/api/cards/**")
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                // Admin reports and listing, including ?customerId=...
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/cards",
                    "/api/cards/reports/blocked",
                    "/api/cards/reports/low-credit"
                ).hasRole("ADMIN")

                // Issue a card
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/cards"
                ).hasRole("ADMIN")

                // Update a card
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/cards/*"
                ).hasRole("ADMIN")

                // Block or unblock a card
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/cards/*/block",
                    "/api/cards/*/unblock"
                ).hasRole("ADMIN")

                // Other existing card operations retain public access.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/cards/reports/most-used",
                    "/api/cards/reports/least-used",
                    "/api/cards/*"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/cards/*/purchase",
                    "/api/cards/*/payment"
                ).permitAll()

                .anyRequest().denyAll()
            )

            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}