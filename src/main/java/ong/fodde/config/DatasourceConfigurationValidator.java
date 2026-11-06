package ong.fodde.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Échoue au démarrage si la datasource n'est pas résolue (évite Tomcat OK puis erreur 30 s plus tard au premier accès DB).
 */
@Component
@Order(0)
public class DatasourceConfigurationValidator implements ApplicationRunner {

    private final Environment env;

    public DatasourceConfigurationValidator(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        String url = env.getProperty("spring.datasource.url");
        String user = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        if (isMissingOrUnresolved(url) || isMissingOrUnresolved(user) || isMissingOrUnresolved(password)) {
            throw new IllegalStateException(
                    "Base de données non configurée : les propriétés spring.datasource.* ne sont pas résolues. "
                            + "Définissez les variables d'environnement SPRING_DATASOURCE_URL, "
                            + "SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD "
                            + "ou créez le fichier src/main/resources/application-local.properties "
                            + "(modèle : application-local.properties.example).");
        }
    }

    private static boolean isMissingOrUnresolved(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String v = value.trim();
        if (v.startsWith("${")) {
            return true;
        }
        return v.contains("${SPRING_DATASOURCE_");
    }
}
