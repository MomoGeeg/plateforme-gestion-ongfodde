package ong.fodde.modules.rh.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ong.fodde.modules.rh.service.ContratService;

/**
 * Exécution unique ~30 s après le démarrage pour tester les échéances contrats (optionnel).
 * Désactivé par défaut pour éviter une erreur « surprise » si la base n'est pas encore configurée.
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.contrat-check-on-startup", havingValue = "true")
public class ContratSchedulerStartupProbe {

    private final ContratService contratService;

    public ContratSchedulerStartupProbe(ContratService contratService) {
        this.contratService = contratService;
    }

    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE)
    public void verifierEcheancesAuDemarrage() {
        contratService.verifierEcheancesContrats();
    }
}
