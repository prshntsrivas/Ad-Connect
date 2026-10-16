package com.adconnect.campaign.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Deal lifecycle:
 * <pre>
 * APPLIED (influencer pitch) ─┐            ┌─> REJECTED
 * OFFERED (brand invite)  ────┴─> ACCEPTED ─┼─> FUNDED (escrow paid) ─> DELIVERED ─> COMPLETED
 *                                           └─> CANCELLED                  └─(revision)─> FUNDED
 * </pre>
 */
public enum DealStatus {
    APPLIED,
    OFFERED,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    FUNDED,
    DELIVERED,
    COMPLETED;

    public Set<DealStatus> next() {
        return switch (this) {
            case APPLIED, OFFERED -> EnumSet.of(ACCEPTED, REJECTED, CANCELLED);
            case ACCEPTED -> EnumSet.of(FUNDED, CANCELLED);
            case FUNDED -> EnumSet.of(DELIVERED);
            case DELIVERED -> EnumSet.of(COMPLETED, FUNDED);
            case REJECTED, CANCELLED, COMPLETED -> EnumSet.noneOf(DealStatus.class);
        };
    }

    public boolean canMoveTo(DealStatus target) {
        return next().contains(target);
    }

    public boolean isActive() {
        return this == ACCEPTED || this == FUNDED || this == DELIVERED;
    }
}
