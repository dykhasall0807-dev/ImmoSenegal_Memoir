package sn.immosenegal.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(TropDeTentativesException.class)
    ResponseEntity<ApiError> tropDeTentatives(TropDeTentativesException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSecondes()))
            .body(ApiError.of(429, "Trop de tentatives", ex.getMessage()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> metier(ApiException ex) {
        HttpStatus statut = ex.getStatus();
        return ResponseEntity.status(statut).body(ApiError.of(statut.value(), statut.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> corpsIllisible(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(400, "Requête invalide", "Le corps de la requête est illisible ou contient une valeur inconnue."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> parametreInvalide(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(400, "Requête invalide", "Valeur invalide pour le paramètre « " + ex.getName() + " »."));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> fichierTropGros(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ApiError.of(413, "Fichier trop volumineux", "Le fichier dépasse la taille autorisée (5 Mo)."));
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    ResponseEntity<ApiError> partieManquante(org.springframework.web.multipart.support.MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(400, "Requête invalide", "Aucun fichier reçu."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflit(DataIntegrityViolationException ex) {
        // Ex. deux inscriptions simultanées avec le même email : on n'expose pas le détail SQL.
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.of(409, "Conflit", "Cette opération entre en conflit avec des données existantes."));
    }
}
