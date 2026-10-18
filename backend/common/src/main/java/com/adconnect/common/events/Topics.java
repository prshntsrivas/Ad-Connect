package com.adconnect.common.events;

/** Kafka topic names shared by producers and consumers. */
public final class Topics {

    public static final String USER_REGISTERED = "adconnect.user.registered";
    public static final String USER_STATUS_CHANGED = "adconnect.user.status-changed";
    public static final String INFLUENCER_PROFILE = "adconnect.influencer.profile";
    public static final String CAMPAIGN = "adconnect.campaign";
    public static final String DEAL_STATUS = "adconnect.deal.status";
    public static final String PAYMENT = "adconnect.payment";
    public static final String CHAT_MESSAGE = "adconnect.chat.message";

    private Topics() {
    }
}
