package sn.immosenegal.common;

import org.springframework.http.HttpStatus;

/** Erreur métier avec le code HTTP à renvoyer. Le message est destiné à l'utilisateur final. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
