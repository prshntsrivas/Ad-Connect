package com.adconnect.campaign.validation;

import com.adconnect.campaign.dto.CampaignDtos.CampaignRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, CampaignRequest> {

    @Override
    public boolean isValid(CampaignRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.startDate() != null && value.endDate() != null && value.endDate().isBefore(value.startDate())) {
            return false;
        }
        return value.applicationDeadline() == null || value.endDate() == null
                || !value.applicationDeadline().isAfter(value.endDate());
    }
}
