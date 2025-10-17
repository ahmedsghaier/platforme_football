package com.example.platforme_backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Désactive CSRF (utile pour API REST)
                .cors(cors -> {})              // Active la gestion CORS (prend en compte CorsConfig)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/players/**").permitAll()
                        .requestMatchers("/api/dashboard/**").permitAll()// Routes publiques
                        .anyRequest().authenticated()                 // Autres routes sécurisées
                );
        return http.build();
    }
}
