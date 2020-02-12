package com.dotcommonitor.plugins;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.Validate;

/**
 *
 * @author dotcom-monitor.com
 */
public class StressTestAction implements Action {

    private final static String ICON_FILENAME = "graph.gif";
    private final static String URL_NAME = "stresstestresults";
    private final AbstractBuild<?, ?> build;
    private final long startTime;
    private final double avgDuration;
    private final long finishedTime;
    private final long sessionsCountSucceded;
    private final long sessionsCountFailed;
    private final long sessionsCountUncompleted;
    private final long sessionsCount;
    private final int errorThreshold;
    private final int avgTimeThreshold;
    private final int duration;
    private final int maxUsers;
    private final StressTestPlatform platformType;
    private final StressTestPlanType planType;
    private final String scenario;
    private final String deviceName;
    private final String report;
    
    
    
    public StressTestAction(AbstractBuild<?, ?> currentBuild, StressTestPluginData data, int threshold, int avgTime, 
            StressTestSettings settings, String scenarioName, String reportUrl) {

        Validate.notNull(currentBuild);
        Validate.notNull(data);
        Validate.notNull(settings);
        Validate.notEmpty(scenarioName);
        Validate.notEmpty(reportUrl);
        
        build = currentBuild;
        startTime = data.getStartTime();
        finishedTime = data.getFinishedTime();
        sessionsCountSucceded = data.getSessionsCountSucceded();
        sessionsCountFailed = data.getSessionsCountFailed();
        sessionsCountUncompleted = data.getSessionsCountUncompleted();
        sessionsCount = data.getSessionsCount();
        avgDuration = data.getAverageDuration();
        errorThreshold = threshold;
        avgTimeThreshold = avgTime;
        duration = settings.getDuration();
        maxUsers = settings.getMaxUsers();
        platformType = settings.getPlatform();
        planType = settings.getPlanType();
        scenario = scenarioName;
        deviceName = settings.getDeviceName();
        report = reportUrl;
    }
    
    
    public AbstractBuild<?, ?> getOwner() {
        return build;
    }
    
    
    @Override
    public String getIconFileName() {
        return ICON_FILENAME;
    }

    
    @Override
    public String getDisplayName() {
        return Messages.StressTestPublisher_TestResults();
    }

    
    @Override
    public String getUrlName() {
        return URL_NAME;
    }
    
    
    public String getStartedTime() {
        return convertTime(startTime);
    }

    
    public String getStoppedTime() {
        return convertTime(finishedTime);
    }
    
    
    public String getDuration() {
        int min = duration / 60;
        int sec = duration % 60;
        String message = (sec > 0) ? "%d min : %d sec" : "%d min";
        message = String.format(message, min, sec);
        return message;
    }
    
    
    public int getMaxUsers() {
        return maxUsers;
    }
    
    
    public long getTotal() {
        return sessionsCount;
    }

    
    public long getSucceeded() {
        return sessionsCountSucceded;
    }
    
    
    public long getFailed() {
        return sessionsCountFailed;
    }
    
    
    public long getUncompleted() {
        return sessionsCountUncompleted;
    }
    
    
    public double getErrors() {
        if (sessionsCount == 0) {
            return 0;
        }
        double percentage = ((double)sessionsCount - sessionsCountSucceded) / sessionsCount * 100;
        double result = Math.floor(percentage * 10) / 10; // truncate value
        return result;
    }
    
    
    public double getAvgDuration() {
        double seconds = avgDuration / 1000;
        double result = Math.floor(seconds * 10) / 10; // truncate value
        return result;
    }
    
    
    public int getErrorThreshold() {
        return errorThreshold;
    }
    
    
    public int getAvgTimeThreshold() {
        return avgTimeThreshold;
    }
    
    
    public int getHeaderType() {
        
        if (getTotal() <= 0)
            return 3; // no data
        
        Boolean showErrors = (errorThreshold == 0) ? getErrors() > 0 : (getErrors() >= errorThreshold);
        Boolean showDuration = getAvgDuration() > avgTimeThreshold;
        
        if (showErrors && showDuration) {
            return 3;
        }
        if (!showErrors && showDuration) {
            return 2;
        }
        if (showErrors && !showDuration) {
            return 1;
        }
        return 0;
    }
    
    
    public Boolean isSuccess() {
        return getHeaderType() == 0;
    }
    
    
    public String getErrorReasone() {
        String result = "";
        double tempErrors = getErrors();
        double tempAvgDuration = getAvgDuration();

        Boolean showErrors = (errorThreshold == 0) ? tempErrors > 0 : (tempErrors >= errorThreshold);
        if (showErrors) {
            result = Messages.StressTestPublisher_FailureReasoneTemplate() + 
                String.format(Messages.StressTestPublisher_ErrorTemplate(), tempErrors, errorThreshold);
        }
        
        Boolean showDuration = tempAvgDuration > avgTimeThreshold;
        if (showDuration) {
            if (showErrors) {
                result += ", " + String.format(Messages.StressTestPublisher_AverageDurationTemplate(), tempAvgDuration, avgTimeThreshold);
            }
            else {
                result = Messages.StressTestPublisher_FailureReasoneTemplate() + 
                    String.format(Messages.StressTestPublisher_AverageDurationTemplate(), tempAvgDuration, avgTimeThreshold);
            }
        }
        
        return result;
    }
    
    
    public int getCurveType() {
        if (planType == StressTestPlanType.DYNAMIC) {
            return 2;
        }
        if (planType == StressTestPlanType.GOAL_TRANSACION) {
            return 1;
        }
        return 0; // StressTestPlanType.STATIC
    }
    
    
    public int getPlatformType() {
        if (platformType == StressTestPlatform.USER_VIEW) {
            return 2;
        }
        if (platformType == StressTestPlatform.BROWSER_VIEW) {
            return 1;
        }
        return 0; //StressTestPlatform.SERVER_VIEW
    }
    
    
    public String getScenarioName() {
        return scenario;
    }
    
    
    public String getDeviceName() {
        return deviceName;
    }
    
    
    public String getReportUrl() {
        return report;
    }
    
    
    private String convertTime(long time) {
        Date date = new Date(time);
        SimpleDateFormat formatDate = new SimpleDateFormat("dd MMM yyyy HH:mm:ss"); // convert to local jenkins server time
        //formatDate.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatDate.format(date);
    }
    
    
    
}
