package booking.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = AddressValidation.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Address {
    String message() default "is incorrect must be: City, Street, number house";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
