package com.dotcommonitor.plugins;

import org.apache.commons.lang.Validate;

/**
 *
 * @author dotcom-monitor.com
 */
public enum StressTestTaskStatus {
    
    SCHEDULED("Scheduled"),
    SUBMITTING_TEST_REQUEST("SubmittingTestRequest"),
    INITIALIZING_TEST("InitializingTest"),
    STARTING_LOAD_INJECTORS("StartingLoadInjectors"),
    SENDING_EXECUTION_PLAN("SendingExecutionPlan"),
    RUNNING("Running"),
    PREPARING_REPORT("PreparingReport"),
    CANCELLING("Cancelling"),
    CANCELLED("Cancelled"),
    FINISHED("Finished");
    
    
    private final String data;
    
    
    public static StressTestTaskStatus fromString(String offer) {
        for (StressTestTaskStatus type : values()) {
            if (type.data.equalsIgnoreCase(offer)) {
                return type;
            }
        }
        throw new IllegalArgumentException( String.format( "Unable to decode \"%s\" as an element type", offer ) );
    }

    
    StressTestTaskStatus(String offer) {
        Validate.notEmpty(offer);
        data = offer;
    }


    @Override
    public String toString() {
        return data;
    }
    
}
