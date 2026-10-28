package ong.fodde.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthentificationException extends RuntimeException {

    public AuthentificationException(String message) {
        super(message);
    }
}
