package com.dotcommonitor.plugins;

import static com.dotcommonitor.plugins.StressTestPlanType.values;
import org.apache.commons.lang.Validate;

/**
 *
 * @author dotcom-monitor.com
 */
public enum StressTestPlatform {
    
    SERVER_VIEW("ServerView"),
    BROWSER_VIEW("BrowserView"),
    USER_VIEW("UserView");
    
    private final String data;
    
    
    public static StressTestPlatform fromString(String offer) {
        for (StressTestPlatform type : values()) {
            if (type.data.equalsIgnoreCase(offer)) {
                return type;
            }
        }
        throw new IllegalArgumentException( String.format( "Unable to decode \"%s\" as an element type", offer ) );
    }

    
    StressTestPlatform(String offer) {
        Validate.notEmpty(offer);
        data = offer;
    }


    @Override
    public String toString() {
        return data;
    }
}
