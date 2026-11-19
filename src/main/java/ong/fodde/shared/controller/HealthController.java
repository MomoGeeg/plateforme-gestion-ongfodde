package ong.fodde.shared.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point de contrôle public, sans accès à la base.
 * Sert aux services de surveillance (UptimeRobot, cron-job.org) qui visitent
 * l'API régulièrement pour l'empêcher de se mettre en veille sur Render.
 * Donne aussi le commit déployé (variable RENDER_GIT_COMMIT fournie par Render),
 * pour vérifier qu'une fusion est bien en ligne avant de publier le frontend.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        String commit = System.getenv("RENDER_GIT_COMMIT");
        return Map.of("statut", "UP", "version", commit == null || commit.isBlank() ? "local" : commit);
    }
}
