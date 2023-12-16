package booking.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class AddressValidation implements ConstraintValidator<Address, String> {
    private static final String PATTERN_OF_ADDRESS = "^[A-Z][a-z]+, [A-Z][a-z]+, \\d+$";

    @Override
    public boolean isValid(String address, final ConstraintValidatorContext context) {
        return address != null && Pattern.compile(PATTERN_OF_ADDRESS)
                .matcher(address)
                .matches();
    }
}
