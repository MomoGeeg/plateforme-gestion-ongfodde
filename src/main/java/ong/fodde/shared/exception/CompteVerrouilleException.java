package ong.fodde.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.LOCKED)
public class CompteVerrouilleException extends RuntimeException {

    public CompteVerrouilleException(String message) {
        super(message);
    }
}
