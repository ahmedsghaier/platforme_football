package com.example.platforme_backend;

import com.example.platforme_backend.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200") // Optionnel si CorsConfig gère cela
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = authService.login(loginRequest.getEmail(), loginRequest.getPassword());

        if (Boolean.TRUE.equals(response.get("success"))) {
            // Générer le token JWT dynamiquement pour l'utilisateur connecté
            String token = jwtUtil.generateToken(loginRequest.getEmail());
            response.put("token", token);
            return ResponseEntity.ok(response);
        }
        // En cas d'échec d'authentification, retourner 401 Unauthorized avec message d'erreur
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Processing logout request");
        try {
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to logout", e);
        }
    }
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> loginWithGoogle(@RequestBody GoogleAuthRequest request) {
        try {
            System.out.println("Tentative de connexion Google avec token: " +
                    (request.getToken() != null ? request.getToken().substring(0, Math.min(20, request.getToken().length())) + "..." : "null"));

            Map<String, Object> response = authService.loginWithGoogle(request.getToken());

            if ((Boolean) response.get("success")) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(response);
            }

        } catch (Exception e) {
            System.err.println("Erreur dans loginWithGoogle: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Erreur serveur lors de la connexion Google"
            ));
        }
    }

    @PostMapping("/google/register")
    public ResponseEntity<Map<String, Object>> registerWithGoogle(@RequestBody GoogleRegisterRequest request) {
        try {
            System.out.println("Tentative d'inscription Google avec token: " +
                    (request.getToken() != null ? request.getToken().substring(0, Math.min(20, request.getToken().length())) + "..." : "null"));

            Map<String, Object> response = authService.registerWithGoogle(
                    request.getToken(),
                    request.getAccountType(),
                    request.getOrganization(),
                    request.isNewsletter(),
                    request.isAcceptTerms()
            );

            if ((Boolean) response.get("success")) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(400).body(response);
            }

        } catch (Exception e) {
            System.err.println("Erreur dans registerWithGoogle: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Erreur serveur lors de l'inscription Google"
            ));
        }
    }

}

// Classes pour les requêtes (inchangées mais incluses pour complétude)
class LoginRequest {
    private String email;
    private String password;

    public LoginRequest() {}
    public LoginRequest(String email, String password) { this.email = email; this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String accountType;
    private String organization;
    private boolean newsletter;
    private boolean acceptTerms;

    public RegisterRequest() {}

    public RegisterRequest(String name, String email, String password, String accountType,
                           String organization, boolean newsletter, boolean acceptTerms) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.accountType = accountType;
        this.organization = organization;
        this.newsletter = newsletter;
        this.acceptTerms = acceptTerms;
    }

    // Getters & Setters pour tous les champs
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public boolean isNewsletter() { return newsletter; }
    public void setNewsletter(boolean newsletter) { this.newsletter = newsletter; }

    public boolean isAcceptTerms() { return acceptTerms; }
    public void setAcceptTerms(boolean acceptTerms) { this.acceptTerms = acceptTerms; }
}
