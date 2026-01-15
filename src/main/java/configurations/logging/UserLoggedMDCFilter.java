package configurations.logging;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

/**
 * This class register the MDC that will be used to maintain the trace related to a user performing actions in the system
 * @author Alexandre Marinho de Souza Júnior
 */
@Provider
@RequiredArgsConstructor
public class UserLoggedMDCFilter implements ContainerRequestFilter {


    private final SecurityContext context;

    @Override
    public void filter(ContainerRequestContext context) {
        String userLogin = extractUser();
        MDC.put("user", userLogin);
    }

    private String extractUser() {
        try {
            return context.getUserPrincipal().getName();
        } catch (Exception e) {
            return "default";
        }
    }

}