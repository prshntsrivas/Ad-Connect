package com.adconnect.campaign.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Campaign end date must not precede the start date, and the application deadline must not be after the end. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    String message() default "endDate must be on or after startDate, and applicationDeadline on or before endDate";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
