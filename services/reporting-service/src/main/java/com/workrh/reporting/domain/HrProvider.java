package com.workrh.reporting.domain;

public enum HrProvider {
    SAP("SAP"),
    WORKDAY("Workday"),
    FACTORIAL("Factorial"),
    BAMBOOHR("BambooHR"),
    LUCCA("Lucca"),
    PAYFIT("Payfit");

    private final String displayName;

    HrProvider(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
