package com.dotcommonitor.plugins;

import org.apache.commons.lang.Validate;

/**
 *
 * @author dotcom-monitor.com
 */
public class StressTestException extends Exception {

    private static final long serialVersionUID = 1L;

    
    public StressTestException(String message) {
        super(message);
        
        Validate.notEmpty(message, "message");
    }

    public StressTestException(String message, Throwable cause) {
        super(message, cause);
        
        Validate.notEmpty(message, "message");
        Validate.notNull(cause, "cause");
    }


}
