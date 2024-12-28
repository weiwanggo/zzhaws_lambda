package com.zzh.aws;

import java.util.List;

public class InstanceTypeUtils {
    public static String upgradeInstanceType(String currentType, List<String> instanceTypes) {
        int currentIndex = instanceTypes.indexOf(currentType);
        if (currentIndex >= 0 && currentIndex < instanceTypes.size() - 1) {
            return instanceTypes.get(currentIndex + 1); // Get the next instance type
        }
        return null; // No upgrade available
    }

    // Method to downgrade the instance type
    public static String downgradeInstanceType(String currentType, List<String> instanceTypes) {
        int currentIndex = instanceTypes.indexOf(currentType);
        if (currentIndex > 0) {
            return instanceTypes.get(currentIndex - 1); // Get the previous instance type
        }
        return null; // No downgrade available
    }
}
