package domain.gateway;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RouteDefinition(
        String id,
        List<String> pathPrefixes,
        boolean stripPrefix,
        TargetDefinition target
) {

    public RouteDefinition {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(pathPrefixes, "pathPrefixes must not be null");
        Objects.requireNonNull(target, "target must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (pathPrefixes.isEmpty()) {
            throw new IllegalArgumentException("pathPrefixes must not be empty");
        }
        if (pathPrefixes.stream().anyMatch(p -> p == null || p.isBlank())) {
            throw new IllegalArgumentException("pathPrefixes must not contain null/blank values");
        }
    }

    public Optional<String> bestMatchingPrefix(String requestPath) {
        if (requestPath == null) {
            return Optional.empty();
        }
        return pathPrefixes.stream()
                .filter(requestPath::startsWith)
                .max(Comparator.comparingInt(String::length));
    }

    public String rewritePath(String requestPath, String matchedPrefix) {
        if (!stripPrefix) {
            return requestPath;
        }

        if (matchedPrefix == null || matchedPrefix.isBlank()) {
            return requestPath;
        }

        String rewritten = requestPath.substring(Math.min(matchedPrefix.length(), requestPath.length()));
        if (rewritten.isEmpty()) {
            return "/";
        }
        if (!rewritten.startsWith("/")) {
            return "/" + rewritten;
        }
        return rewritten;
    }
}
