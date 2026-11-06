package ong.fodde.shared.exception;

/** Erreur du service de stockage des fichiers (Supabase Storage injoignable, refus, non configuré). */
public class StockageException extends RuntimeException {

    public StockageException(String message) {
        super(message);
    }

    public StockageException(String message, Throwable cause) {
        super(message, cause);
    }
}
