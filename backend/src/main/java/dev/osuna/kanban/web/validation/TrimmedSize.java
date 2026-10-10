package dev.osuna.kanban.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Like {@link jakarta.validation.constraints.Size} but validates the <b>trimmed</b>
 * value: SPEC 7.3 defines name/title length "tras trim" (after trimming).
 * <p>The lower bound (at least one non-whitespace character) is enforced by
 * {@link jakarta.validation.constraints.NotBlank}; this constraint only caps the
 * length of the trimmed value, mirroring {@code @Size(max = ...)}. Null and
 * whitespace-only values are left to {@code @NotBlank} so a field reports a single
 * error (the order of multiple constraint errors is unspecified by Bean Validation).
 * <p>The stored value is trimmed by the service layer, so the validated (trimmed)
 * length always matches what is persisted (SPEC 7.3).
 */
@Documented
@Constraint(validatedBy = TrimmedSize.TrimmedSizeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TrimmedSize {

    /** Upper bound of the trimmed value's length. */
    int max();

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Validator reading {@link #max()} from the annotation at initialization time. */
    class TrimmedSizeValidator implements ConstraintValidator<TrimmedSize, String> {

        private int max;

        @Override
        public void initialize(TrimmedSize annotation) {
            this.max = annotation.max();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.trim().length() <= max;
        }
    }
}
