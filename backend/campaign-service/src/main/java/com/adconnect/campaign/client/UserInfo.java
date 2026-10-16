package com.adconnect.campaign.client;

public record UserInfo(Long id, String email, String fullName, String role, String status) {

    public boolean isVerified() {
        return "VERIFIED".equals(status);
    }
}
