package configurations.health;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;


/**
 * @author Alexandre Marinho de Souza Júnior on 17/08/2022
 */

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class AppLifecycleBean {

    @ConfigProperty(name = "greeting.profile", defaultValue = "production")
    String profile;



    void onStart(@Observes StartupEvent ev) {
        log.info("Iniciando a aplicação com o perfil: {}", profile);
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("A aplicação está se encerrando...");
    }

}
