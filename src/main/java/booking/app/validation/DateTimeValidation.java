package booking.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class DateTimeValidation implements ConstraintValidator<DateTime, String> {
    private static final String PATTERN_OF_DATE =
            "^[1-9]\\d{3}, (0?[1-9]|1[0-2]), (0?[1-9]|[12]\\d|3[01])$";

    @Override
    public boolean isValid(final String date, final ConstraintValidatorContext context) {
        return date != null && Pattern.compile(PATTERN_OF_DATE)
                .matcher(date)
                .matches();
    }
}
