package ong.fodde.shared.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point de contrôle public, sans accès à la base.
 * Sert aux services de surveillance (UptimeRobot, cron-job.org) qui visitent
 * l'API régulièrement pour l'empêcher de se mettre en veille sur Render.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("statut", "UP");
    }
}
