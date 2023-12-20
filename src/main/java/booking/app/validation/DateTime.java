package booking.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DateTimeValidation.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DateTime {
    String message() default
            "these data are incorrect, you should write the correct data month no more than: "
                    + "12, day no more than: 31, here is an example: 2023, 12, 15";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
