package ong.fodde.modules.rh.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ong.fodde.modules.rh.service.ContratService;

@Component
public class ContratScheduler {

    private final ContratService contratService;

    public ContratScheduler(ContratService contratService) {
        this.contratService = contratService;
    }

    /**
     * Vérification des échéances contrats — tous les jours à 8h00
     * Cron : seconde minute heure jour mois jour-semaine
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void verifierEcheancesContrats() {
        contratService.verifierEcheancesContrats();
    }

}