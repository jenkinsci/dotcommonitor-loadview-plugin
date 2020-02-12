package com.dotcommonitor.plugins;

import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import org.apache.commons.lang.Validate;

/**
 *
 * @author dotcom-monitor.com
 */
public class StressTestUrlResolver {
    
    private final static String KEY_URL_BASE = "DM_ST_KEY_API";
    private final static String KEY_REPORT = "DM_ST_KEY_REPORT";
    private final static String URL_BASE = "https://api.dotcom-monitor.com/config_api_v1/";
    private final static String URL_REPORT_TEMPLATE = "https://user.dotcom-monitor.com/StressReport.aspx?id=%d";
    private final static String URL_POST_LOGIN = "LogIn?TypeUID=LogInAsLvWebApi";
    private final static String URL_POST_RUN = "StressTest/%d/Run";
    private final static String URL_GET_CLONE = "StressTest/%d/Clone";
    private final static String URL_GET_STATUS = "StressTest/%d";
    private final static String URL_GET_RESULT = "StressTest/%d/Result";
    private final static String URL_GET_ABORT = "StressTest/%d/AbortTest";
    private final String urlBase;
    private final String urlReportTemplate;
    
    
    public StressTestUrlResolver() {
        urlBase = resolveUrl(KEY_URL_BASE, URL_BASE);
        urlReportTemplate = resolveUrl(KEY_REPORT, URL_REPORT_TEMPLATE);
    }
    
    public String getUrlReport(int deviceId) {
        return String.format(urlReportTemplate, deviceId);
    }
    
    public String getUrlPostLogin() {
        return urlBase + URL_POST_LOGIN;
    }
    
    public String getUrlPostRun(int deviceId) {
        return String.format(urlBase + URL_POST_RUN, deviceId);
    }
    
    public String getUrlGetClone(int deviceId) {
        return String.format(urlBase + URL_GET_CLONE, deviceId);
    }
    
    public String getUrlGetStatus(int deviceId) {
        return String.format(urlBase + URL_GET_STATUS, deviceId);
    }
    
    public String getUrlGetResult(int deviceId) {
        return String.format(urlBase + URL_GET_RESULT, deviceId);
    }
    
    public String getUrlGetAbort(int deviceId) {
        return String.format(urlBase + URL_GET_ABORT, deviceId);
    }
    
    
    private String resolveUrl(String key, String defaultValue) {
        Validate.notEmpty(key);    
        Validate.notEmpty(defaultValue);
        
        try {
            DescribableList<NodeProperty<?>, NodePropertyDescriptor> props = Jenkins.getInstance().getGlobalNodeProperties();
            if (props == null || props.size() <= 0)
                return defaultValue;

            for(Object obj : props) {
                if (obj == null || !(obj instanceof EnvironmentVariablesNodeProperty))
                    continue;
            
                EnvironmentVariablesNodeProperty property = (EnvironmentVariablesNodeProperty)obj;
                EnvVars envVars = property.getEnvVars();
                if (envVars == null)
                    continue;
            
                Object value = envVars.get(key);
                if (value != null && value instanceof String)
                    return (String)value;
            }
        }
        catch (Exception ex) {
            // this method must be safe
        }
        
        return defaultValue;
    }
    
    
    
}
