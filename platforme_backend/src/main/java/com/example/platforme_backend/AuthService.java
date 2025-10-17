package com.example.platforme_backend;

import com.example.platforme_backend.User;
import com.example.platforme_backend.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GoogleOAuthService googleOAuthService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> register(String name, String email, String password,
                                        String accountType, String organization,
                                        boolean newsletter, boolean acceptTerms) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> existingUser = userRepository.findByEmail(email);
        System.out.println("Recherche utilisateur pour email " + email + " : " + existingUser);

        if (existingUser.isPresent()) {
            response.put("success", false);
            response.put("message", "Cet email est déjà utilisé.");
            return response;
        }

        // Créer un nouvel utilisateur
        String[] parts = name.split(" ", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setAccountType(accountType);
        user.setOrganization(organization);
        user.setNewsletter(newsletter);
        user.setAcceptTerms(acceptTerms);
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        userRepository.save(user);
        return Map.of("success", true);
    }


    public Map<String, Object> login(String email, String password) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent() && passwordEncoder.matches(password, userOptional.get().getPassword())) {
            User user = userOptional.get();
            response.put("success", true);
            response.put("message", "Connexion réussie");
            response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getFirstName() + " " + user.getLastName()
            ));
        } else {
            response.put("success", false);
            response.put("message", "Email ou mot de passe incorrect");
        }

        return response;
    }
    public Map<String, Object> loginWithGoogle(String googleToken) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Vérifier le token Google
            GoogleOAuthService.GoogleUserInfo googleUser = googleOAuthService.verifyGoogleToken(googleToken);

            // Chercher l'utilisateur par email
            Optional<User> userOptional = userRepository.findByEmail(googleUser.email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Générer un token JWT (vous devez implémenter cette partie)
                String jwtToken = generateJwtToken(user);

                response.put("success", true);
                response.put("message", "Connexion Google réussie");
                response.put("token", jwtToken);
                response.put("user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getFirstName() + " " + user.getLastName(),
                        "accountType", user.getAccountType()
                ));
            } else {
                response.put("success", false);
                response.put("message", "Aucun compte trouvé avec cet email Google. Veuillez vous inscrire d'abord.");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la connexion Google: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Échec de la connexion Google: " + e.getMessage());
        }

        return response;
    }

    public Map<String, Object> registerWithGoogle(String googleToken, String accountType,
                                                  String organization, boolean newsletter,
                                                  boolean acceptTerms) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Vérifier le token Google
            GoogleOAuthService.GoogleUserInfo googleUser = googleOAuthService.verifyGoogleToken(googleToken);

            // Vérifier si l'utilisateur existe déjà
            Optional<User> existingUser = userRepository.findByEmail(googleUser.email);
            if (existingUser.isPresent()) {
                response.put("success", false);
                response.put("message", "Un compte existe déjà avec cet email.");
                return response;
            }

            // Créer un nouvel utilisateur
            User user = new User();
            user.setFirstName(googleUser.given_name != null ? googleUser.given_name : googleUser.name);
            user.setLastName(googleUser.family_name != null ? googleUser.family_name : "");
            user.setEmail(googleUser.email);
            user.setPassword(""); // Pas de mot de passe pour les utilisateurs Google
            user.setAccountType(accountType);
            user.setOrganization(organization != null ? organization : "");
            user.setNewsletter(newsletter);
            user.setAcceptTerms(acceptTerms);
            user.setGoogleId(googleUser.id); // Ajoutez ce champ à votre entité User
            user.setCreatedAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Inscription Google réussie");

        } catch (Exception e) {
            System.err.println("Erreur lors de l'inscription Google: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Échec de l'inscription Google: " + e.getMessage());
        }

        return response;
    }
    private String generateJwtToken(User user) {
        // Implémentez votre logique JWT ici
        // Pour l'instant, retournez un token factice
        return "jwt-token-for-user-" + user.getId();
    }

}