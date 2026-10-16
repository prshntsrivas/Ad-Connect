package com.adconnect.campaign.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Role-specific summary. For brands {@code money} is total committed spend; for influencers it is
 * earnings; for staff it is platform gross merchandise value.
 */
public record DashboardDto(String role, Map<String, Long> campaignsByStatus, Map<String, Long> dealsByStatus,
                           BigDecimal money, BigDecimal pendingMoney, Double rating, Integer completedDeals,
                           boolean profileComplete) {
}
