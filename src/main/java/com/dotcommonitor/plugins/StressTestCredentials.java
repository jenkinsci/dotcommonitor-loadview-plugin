package com.dotcommonitor.plugins;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.Secret;
import java.io.IOException;

/**
 *
 * @author dotcom-monitor.com
 */
public interface StressTestCredentials  extends  StandardCredentials {

    Secret getApiKey() throws IOException, InterruptedException; 
    
}
