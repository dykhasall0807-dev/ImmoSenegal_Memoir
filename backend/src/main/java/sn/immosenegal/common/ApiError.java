package sn.immosenegal.common;

import java.time.Instant;
import java.util.Map;

/** Format unique des erreurs renvoyées par l'API. Ne contient jamais de détail technique interne. */
public record ApiError(int status, String error, String message, Instant timestamp, Map<String, String> details) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now(), Map.of());
    }

    public static ApiError of(int status, String error, String message, Map<String, String> details) {
        return new ApiError(status, error, message, Instant.now(), details);
    }
}
