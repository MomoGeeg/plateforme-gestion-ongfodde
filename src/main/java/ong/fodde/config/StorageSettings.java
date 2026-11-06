package ong.fodde.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Stockage des fichiers GED dans Supabase Storage.
 * La clé de service reste côté serveur : le frontend ne parle jamais à Supabase.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageSettings {

    /** URL du projet Supabase, ex. https://abcd.supabase.co (variable SUPABASE_URL). */
    private String supabaseUrl;

    /** Clé service_role du projet (variable SUPABASE_SERVICE_KEY). Ne jamais l'exposer au frontend. */
    private String serviceKey;

    /** Bucket privé qui contient les fichiers (variable SUPABASE_STORAGE_BUCKET, défaut « ged »). */
    private String bucket = "ged";

    public boolean estConfigure() {
        return supabaseUrl != null && !supabaseUrl.isBlank()
                && serviceKey != null && !serviceKey.isBlank()
                && bucket != null && !bucket.isBlank();
    }
}
