package application.dtos.gateway;

import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DownstreamResponse(
        int status,
        Map<String, List<String>> headers,
        byte[] body
) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DownstreamResponse that = (DownstreamResponse) obj;
        return status == that.status && headers.equals(that.headers) && Arrays.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, headers, Arrays.hashCode(body));
    }

    @Override
    public @NonNull String toString() {
        return "DownstreamResponse{" +
                "status=" + status +
                ", headers=" + headers +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
