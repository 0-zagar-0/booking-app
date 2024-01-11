package booking.app.exception;

public class TelegramException extends RuntimeException {
    public TelegramException(final String message) {
        super(message);
    }
}
