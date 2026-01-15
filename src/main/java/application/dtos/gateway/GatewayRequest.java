package application.dtos.gateway;

import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GatewayRequest(
        String method,
        String path,
        String rawQuery,
        Map<String, List<String>> headers,
        byte[] body
) {

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GatewayRequest that = (GatewayRequest) obj;
        return method.equals(that.method) && path.equals(that.path) && rawQuery.equals(that.rawQuery)
                && headers.equals(that.headers) && Arrays.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, rawQuery, headers, Arrays.hashCode(body));
    }

    @Override
    public @NonNull String toString() {
        return "GatewayRequest{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", rawQuery='" + rawQuery + '\'' +
                ", headers=" + headers +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
