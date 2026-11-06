package ong.fodde.config;

import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SecuritySettings securitySettings;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, SecuritySettings securitySettings) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.securitySettings = securitySettings;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> {
                // Les erreurs (403, 400, 500…) sont renvoyées en interne vers /error : sans ceci,
                // toute erreur devenait un 401 « Non authentifié » et le frontend déconnectait l'utilisateur.
                auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll();
                auth.requestMatchers("/error").permitAll();
                auth.requestMatchers("/api/auth/login").permitAll();
                auth.requestMatchers(HttpMethod.GET, "/api/health").permitAll();
                if (securitySettings.isInitEndpointEnabled()) {
                    auth.requestMatchers("/api/auth/init").permitAll();
                } else {
                    auth.requestMatchers("/api/auth/init").denyAll();
                }
                auth.anyRequest().authenticated();
            })
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"erreur\":\"Non authentifié\"}");
                })
                // Connecté mais rôle insuffisant : 403 (pas de déconnexion côté frontend)
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"erreur\":\"Accès refusé : droits insuffisants\"}");
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Patterns : accepte les origines exactes et les jokers (ex. https://*.vercel.app pour les previews)
        config.setAllowedOriginPatterns(securitySettings.corsAllowedOriginsList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Permet au frontend de lire le nom du fichier téléchargé
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}