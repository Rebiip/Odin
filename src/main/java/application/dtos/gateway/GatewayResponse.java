package application.dtos.gateway;

import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GatewayResponse(
        int status,
        Map<String, List<String>> headers,
        byte[] body
) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GatewayResponse that = (GatewayResponse) obj;
        return status == that.status && headers.equals(that.headers) && Arrays.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(status, headers);
        result = 31 * result + Arrays.hashCode(body);
        return result;
    }

    @Override
    public @NonNull String toString() {
        return "GatewayResponse{" +
                "status=" + status +
                ", headers=" + headers +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
