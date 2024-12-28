package com.zzh.aws;

public class InstanceUpdateRequest {
    public static final String UPGRADE_TYPE = "upgrade";
    public static final String DOWNGRADE_TYPE = "downgrade";

    private String newInstanceType;

    public String getNewInstanceType() {
        return newInstanceType;
    }

    public void setNewInstanceType(String newInstanceType) {
        this.newInstanceType = newInstanceType;
    }
}
