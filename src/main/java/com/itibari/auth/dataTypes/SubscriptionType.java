package com.itibari.auth.dataTypes;

public enum SubscriptionType {
    Free_Trial("Free Trial Subscription"),
    Single_Shop("Single Shop Subscription"),
    Enterprise("Enterprise Subscription");

    private final String label;

    SubscriptionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
