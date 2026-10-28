package sn.immosenegal.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Transforme les erreurs courantes en réponses JSON claires. Les autres erreurs restent gérées
 * par Spring (sans message ni stacktrace grâce à application.yml).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> details.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(400, "Requête invalide", "Certains champs sont invalides.", details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accesInterdit(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError.of(403, "Accès interdit", "Vous n'avez pas le droit d'effectuer cette action."));
    }
}
