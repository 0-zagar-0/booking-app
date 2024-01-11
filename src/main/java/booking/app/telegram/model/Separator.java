package booking.app.telegram.model;

import lombok.Getter;

@Getter
public enum Separator {
    COMMA(","),
    COLON(":"),
    SPACE(" "),
    PIPE("|"),
    EQUALS("="),
    L_SQUARE_BRACKET("[");

    private final String separator;

    Separator(String separator) {
        this.separator = separator;
    }
}
