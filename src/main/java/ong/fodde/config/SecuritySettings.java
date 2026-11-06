package ong.fodde.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Paramètres de sécurité applicative (CORS, bootstrap admin).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecuritySettings {

    /**
     * Si true, {@code POST /api/auth/init} est autorisé sans authentification.
     * À garder désactivé en production sauf fenêtre de bootstrap contrôlée.
     */
    private boolean initEndpointEnabled = false;

    /**
     * Origines autorisées pour CORS, séparées par des virgules (ex. {@code http://localhost:5173,https://app.example.com}).
     */
    private String corsAllowedOrigins = "http://localhost:5173";

    public List<String> corsAllowedOriginsList() {
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
