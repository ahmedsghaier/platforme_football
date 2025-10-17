package com.example.platforme_backend;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@Service
public class GoogleOAuthService {

    private static final String CLIENT_ID = "750375178874-j8l8kmoi61kqqo1p4a5ui8b5sha97unf.apps.googleusercontent.com";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleUserInfo {
        public String id;
        public String email;
        public String name;
        public String given_name;
        public String family_name;
        public String picture;
        public boolean verified_email;
    }

    public GoogleUserInfo verifyGoogleToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
                    GOOGLE_USERINFO_URL,
                    HttpMethod.GET,
                    entity,
                    GoogleUserInfo.class
            );

            GoogleUserInfo userInfo = response.getBody();

            if (userInfo == null || userInfo.email == null) {
                throw new RuntimeException("Invalid token or missing user info");
            }

            return userInfo;

        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du token Google: " + e.getMessage());
            throw new RuntimeException("Token Google invalide", e);
        }
    }
}
