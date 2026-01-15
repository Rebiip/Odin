package ports.out.gateway;

import domain.gateway.RouteDefinition;

import java.util.List;

public interface RouteDefinitionsPort {
    List<RouteDefinition> listRoutes();
}
