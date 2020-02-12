package com.dotcommonitor.plugins;

import org.apache.commons.lang.Validate;
import org.json.JSONObject;

/**
 *
 * @author dotcom-monitor.com
 */
public class StressTestPluginData {
    
    private final static String TAG_HAS_DATA = "HasData";
    private final static String TAG_START_TIME = "StartTime_UTC";
    private final static String TAG_FINISHED_TIME = "FinishedTime_UTC";
    private final static String TAG_SESSIONS_COUNT = "SessionsCount";
    private final static String TAG_SESSIONS_COUNT_SUCCEDED = "SessionsCountSucceded";
    private final static String TAG_SESSIONS_COUNT_FAILED = "SessionsCountFailed";
    private final static String TAG_SESSIONS_COUNT_UNCOMPLETED = "SessionsCountUncompleted";
    private final static String TAG_AVERAGE_DURATION = "AverageDuration";
    
    private final Boolean hasData;
    private final long startTime;
    private final long finishedTime;
    private final double averageDuration;
    private final long sessionsCount;
    private final long sessionsCountSucceded;
    private final long sessionsCountFailed;
    private final long sessionsCountUncompleted;
    
    
    public StressTestPluginData(String body) {
        Validate.notEmpty(body);
    
        JSONObject json = new JSONObject(body);
        
        hasData = json.getBoolean(TAG_HAS_DATA);
        startTime = json.getLong(TAG_START_TIME);
        finishedTime = json.getLong(TAG_FINISHED_TIME);
        sessionsCount = json.getLong(TAG_SESSIONS_COUNT);
        sessionsCountSucceded = json.getLong(TAG_SESSIONS_COUNT_SUCCEDED);
        sessionsCountFailed = json.getLong(TAG_SESSIONS_COUNT_FAILED);
        sessionsCountUncompleted = json.getLong(TAG_SESSIONS_COUNT_UNCOMPLETED);
        averageDuration = json.getDouble(TAG_AVERAGE_DURATION);
    }
    
    
    
    public Boolean getHasData() {
        return hasData;
    }
    
    public long getStartTime() {
        return startTime;
    }

    public long getFinishedTime() {
        return finishedTime;
    }
    
    public long getSessionsCount() {
        return sessionsCount;
    }
    
    public long getSessionsCountSucceded() {
        return sessionsCountSucceded;
    }
    
    public long getSessionsCountFailed() {
        return sessionsCountFailed;
    }
    
    public long getSessionsCountUncompleted() {
        return sessionsCountUncompleted;
    }
    
    public double getAverageDuration() {
        return averageDuration;
    }
    
    
}

