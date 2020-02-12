package com.dotcommonitor.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.json.JSONObject;

/**
 *
 * @author dotcom-monitor.com
 */
public class StressTestSettings {
    
    private final static String TAG_STATUS = "Status";
    private final static String TAG_PLAN_TYPE = "PlanType";
    private final static String TAG_NAME = "Name";
    private final static String TAG_ID = "ID";
    private final static String TAG_PLATFORM = "DevicePlatform";
    private final static String TAG_STATIC_DURATION = "static_Duration";
    private final static String TAG_STATIC_USERS = "static_MaxUsers";
    private final static String TAG_GOALBASED_DURATION = "goalBasedCurve_Duration";
    private final static String TAG_GOALBASED_USERS = "goalBasedCurve_MaxUsers";
    private final static String TAG_DYNAMIC_DURATION = "dynamicCurve_MaxTestDuration";
    private final static String TAG_DYNAMIC_USERS = "dynamicCurve_MaxUsers";
    
    
    private final StressTestTaskStatus status;
    private final StressTestPlanType planType;
    private final StressTestPlatform platform;
    private final String deviceName;
    private final int id;
    private final int duration;
    private final int maxUsers;
    
    
    public StressTestSettings(String body) {
         Validate.notEmpty(body);
    
        JSONObject json = new JSONObject(body);
        
        id = json.getInt(TAG_ID);
        status = StressTestTaskStatus.fromString(json.getString(TAG_STATUS));
        planType = StressTestPlanType.fromString(json.getString(TAG_PLAN_TYPE));
        platform = StressTestPlatform.fromString(json.getString(TAG_PLATFORM));
        
        String temp = json.getString(TAG_NAME);
        deviceName = StringUtils.isBlank(temp) ? Messages.StressTestPublisher_UnknownDevice() : temp;
        
        duration = getDurationInternal(json);
        maxUsers = getMaxUsersInternal(json);
    }
    
    public StressTestPlatform getPlatform() {
        return platform;
    }
    
    public StressTestTaskStatus getStatus() {
        return status;
    }
    
    public StressTestPlanType getPlanType() {
        return planType;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public int getId() {
        return id;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public int getMaxUsers() {
        return maxUsers;
    }
    
    
    private int getDurationInternal(JSONObject json) {
        Validate.notNull(json);
        
        if (planType == StressTestPlanType.STATIC) {
            return json.getInt(TAG_STATIC_DURATION);
        }
        if (planType == StressTestPlanType.GOAL_TRANSACION) {
            return json.getInt(TAG_GOALBASED_DURATION);
        }
        if (planType == StressTestPlanType.DYNAMIC) {
            return json.getInt(TAG_DYNAMIC_DURATION);
        }
        
        throw new RuntimeException(Messages.StressTestPublisher_ResponseBodyEmpty());
    }
    
    
    private int getMaxUsersInternal(JSONObject json) {
        Validate.notNull(json);
        
        if (planType == StressTestPlanType.STATIC) {
            return json.getInt(TAG_STATIC_USERS);
        }
        if (planType == StressTestPlanType.GOAL_TRANSACION) {
            return json.getInt(TAG_GOALBASED_USERS);
        }
        if (planType == StressTestPlanType.DYNAMIC) {
            return json.getInt(TAG_DYNAMIC_USERS);
        }
        
        throw new RuntimeException(Messages.StressTestPublisher_ResponseBodyEmpty());
    }
    
    
}
