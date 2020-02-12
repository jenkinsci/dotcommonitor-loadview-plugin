package com.dotcommonitor.plugins;

import org.apache.commons.lang.Validate;

/**
 *
 * @author dotcom-monitor.com
 */
public enum StressTestPlanType {
 
    STATIC("StaticPlan"),
    DYNAMIC("DynamicPlan"),
    GOAL_TRANSACION("GoalTransaction");
    
    private final String data;
    
    
    public static StressTestPlanType fromString(String offer) {
        for (StressTestPlanType type : values()) {
            if (type.data.equalsIgnoreCase(offer)) {
                return type;
            }
        }
        throw new IllegalArgumentException( String.format( "Unable to decode \"%s\" as an element type", offer ) );
    }

    
    StressTestPlanType(String offer) {
        Validate.notEmpty(offer);
        data = offer;
    }


    @Override
    public String toString() {
        return data;
    }
    
}
