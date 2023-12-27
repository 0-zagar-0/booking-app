package booking.app.exception;

public class CardProcessingException extends RuntimeException {
    public CardProcessingException(final String message) {
        super(message);
    }
}
