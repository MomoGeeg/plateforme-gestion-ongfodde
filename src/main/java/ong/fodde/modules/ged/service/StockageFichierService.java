package ong.fodde.modules.ged.service;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import ong.fodde.config.StorageSettings;
import ong.fodde.shared.exception.StockageException;

/**
 * Accès à Supabase Storage via son API REST, avec la clé de service.
 * Le bucket doit être PRIVÉ : seuls le backend (et donc les contrôles d'accès GED) y accèdent.
 */
@Service
public class StockageFichierService {

    private final StorageSettings settings;
    private final RestClient restClient;

    public StockageFichierService(StorageSettings settings) {
        this.settings = settings;
        this.restClient = RestClient.create();
    }

    public void deposer(String chemin, byte[] contenu, String contentType) {
        verifierConfiguration();
        try {
            restClient.post()
                    .uri(objetUri(chemin))
                    .header("Authorization", "Bearer " + settings.getServiceKey())
                    .header("apikey", settings.getServiceKey())
                    .header("x-upsert", "false")
                    .contentType(mediaType(contentType))
                    .body(contenu)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new StockageException(
                    "Le stockage a refusé le fichier (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (RestClientException e) {
            throw new StockageException("Stockage des fichiers injoignable", e);
        }
    }

    public byte[] lire(String chemin) {
        verifierConfiguration();
        try {
            byte[] contenu = restClient.get()
                    .uri(objetUri(chemin))
                    .header("Authorization", "Bearer " + settings.getServiceKey())
                    .header("apikey", settings.getServiceKey())
                    .retrieve()
                    .body(byte[].class);
            return contenu != null ? contenu : new byte[0];
        } catch (RestClientResponseException e) {
            throw new StockageException(
                    "Fichier introuvable dans le stockage (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (RestClientException e) {
            throw new StockageException("Stockage des fichiers injoignable", e);
        }
    }

    private void verifierConfiguration() {
        if (!settings.estConfigure()) {
            throw new StockageException(
                    "Stockage des fichiers non configuré : définir SUPABASE_URL et SUPABASE_SERVICE_KEY côté serveur.");
        }
    }

    private URI objetUri(String chemin) {
        String base = settings.getSupabaseUrl().trim().replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(base)
                .pathSegment("storage", "v1", "object", settings.getBucket())
                .pathSegment(chemin.split("/"))
                .build()
                .encode()
                .toUri();
    }

    private static MediaType mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
