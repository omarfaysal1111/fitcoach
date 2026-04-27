package com.fitcoach.domain.enums;

import java.math.BigDecimal;

public enum SubscriptionPlan {

    TRIAL(3, 7, BigDecimal.ZERO),
    BASIC(10, 30, new BigDecimal("399")),
    PREMIUM(25, 30, new BigDecimal("699")),
    ELITE(Integer.MAX_VALUE, 30, new BigDecimal("999"));

    private final int maxClients;
    private final int durationDays;
    private final BigDecimal priceEgp;

    SubscriptionPlan(int maxClients, int durationDays, BigDecimal priceEgp) {
        this.maxClients = maxClients;
        this.durationDays = durationDays;
        this.priceEgp = priceEgp;
    }

    public int getMaxClients() { return maxClients; }
    public int getDurationDays() { return durationDays; }
    public BigDecimal getPriceEgp() { return priceEgp; }

    public boolean isUnlimited() { return maxClients == Integer.MAX_VALUE; }
}
