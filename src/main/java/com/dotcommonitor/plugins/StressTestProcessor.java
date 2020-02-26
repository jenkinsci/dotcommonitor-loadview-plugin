package com.dotcommonitor.plugins;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Run;
import hudson.util.Secret;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 *
 * @author dotcom-monitor.com
 */
public class StressTestProcessor {
    
    private static final int WAIT_STEP_TIMEOUT = 5000; // 5 sec.
    private static final int RETRY_TIMEOUT = 1000;
    private static final int RETRY_COUNT = 3;
    private final String credentialsId;
    private final String scenarioId;
    private final Run<?, ?> run;
    private final PrintStream console;
    private final int errorThreshold;
    private final int avgTime;
    private final List<StressTestTaskStatus> provisionalStatuses;
    
    
    public StressTestProcessor(PrintStream consoleStream, Run<?, ?> runBuild, StressTestPublisher publisher) {
        Validate.notNull(consoleStream);
        Validate.notNull(runBuild);
        
        console = consoleStream;
        run = runBuild;
        credentialsId = publisher.getCredentialsId();
        scenarioId = publisher.getScenarioId();
        errorThreshold = publisher.getErrorThreshold();
        avgTime = publisher.getAvgTime();
        provisionalStatuses = new ArrayList<>();
        provisionalStatuses.add(StressTestTaskStatus.INITIALIZING_TEST);
        provisionalStatuses.add(StressTestTaskStatus.STARTING_LOAD_INJECTORS);
        provisionalStatuses.add(StressTestTaskStatus.SENDING_EXECUTION_PLAN);
        provisionalStatuses.add(StressTestTaskStatus.RUNNING);
        provisionalStatuses.add(StressTestTaskStatus.PREPARING_REPORT);
        provisionalStatuses.add(StressTestTaskStatus.CANCELLING);
    }

    
    public boolean process() throws Exception {
        
        int deviceId = getScenarioId();
        String uid = getApiKey();
        StressTestUrlResolver resolver = new StressTestUrlResolver();
        StressTestPluginApi api = new StressTestPluginApi(uid, resolver);
        login(api);
        StressTestSettings settings = getSettings(api, deviceId);
        String scenarioName = settings.getDeviceName();
        int testId = runTest(api, deviceId);

        String reportUrl = String.format(Messages.StressTestPublisher_LinkReportTemplate(), resolver.getUrlReport(testId));
        console.println(reportUrl);
        
        StressTestTaskStatus status = StressTestTaskStatus.SUBMITTING_TEST_REQUEST;
        printStatus(status);
        
        try {
            while(status != StressTestTaskStatus.CANCELLED && 
                status != StressTestTaskStatus.FINISHED) {
            
                settings = getSettings(api, testId);
                status = settings.getStatus();
                printStatuses(status);
                
                if (status == StressTestTaskStatus.CANCELLED ||
                    status == StressTestTaskStatus.FINISHED)
                    break;
            
                if (status == StressTestTaskStatus.SCHEDULED) {
                    String message = String.format(Messages.StressTestPublisher_FailedTemplate(), settings.getDeviceName(), settings.getId(), status.toString());
                    throw new StressTestException(message);   
                }
                
                Thread.sleep(WAIT_STEP_TIMEOUT);
            }
        }
        catch (InterruptedException ex) {
            abort(api, testId);
            api.closeHttpClient();
            throw ex;
        }
        catch (Exception ex) {
            api.closeHttpClient();
            throw ex;
        }
        
        if (settings == null) {
            api.closeHttpClient();
            throw new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
        }
        
        printStatus(status);
        
        if (StressTestTaskStatus.CANCELLED == status) {
            String message = String.format(Messages.StressTestPublisher_CancelledTemplate(), settings.getDeviceName(), settings.getId());
            api.closeHttpClient();
            throw new StressTestException(message);
        }
        
        if (StressTestTaskStatus.FINISHED != status) {
            String message = String.format(Messages.StressTestPublisher_FailedTemplate(), settings.getDeviceName(), settings.getId(), status.toString());
            api.closeHttpClient();
            throw new StressTestException(message);
        }
        
        // StressTestTaskStatus.FINISHED == status
        
        StressTestPluginData data = getResult(api, testId);
        api.closeHttpClient();
        
        if (data == null || !data.getHasData()) {
            throw new RuntimeException(Messages.StressTestPublisher_ResponseBodyEmpty()); 
        }
        
        if (data.getSessionsCount() <= 0) /* Fatal error */ {
            String message = String.format(Messages.StressTestPublisher_FailedTemplate(), settings.getDeviceName(), settings.getId(), status.toString());
            throw new StressTestException(message);
        }
        
        StressTestAction action = new StressTestAction(run, data, errorThreshold, avgTime, 
                settings, scenarioName, resolver.getUrlReport(testId));
        run.addAction(action);
        
        Boolean success = action.isSuccess();
        
        if (!success) {
            String errorDescription = action.getErrorReasone();
            if (!StringUtils.isBlank(errorDescription)) {
                console.println(errorDescription);
            }
        }
        
        return success;
    }
 
    
    private String getApiKey() throws Exception {
        
        StressTestCredentials credentials = CredentialsProvider.findCredentialById(
            credentialsId, 
            StressTestCredentials.class, 
            run, 
            Collections.<DomainRequirement>emptyList());
        
        if (credentials == null) {
            throw new RuntimeException(Messages.StressTestPublisher_GetCredentialsError());
        }

        Secret secret = credentials.getApiKey();
        String apiKey = secret != null ? secret.getPlainText() : "";
        
        if (StringUtils.isBlank(apiKey)) {
            throw new RuntimeException(Messages.StressTestPublisher_GetAPIKeyError());
        }
    
        return apiKey;
    }
    
    
    private int getScenarioId() {

        try {
            int result = Integer.parseInt(scenarioId);
            return result;
        }
        catch(NumberFormatException ex) {
            RuntimeException error = new RuntimeException(Messages.StressTestPublisher_GetScenarioIdError(), ex);
            throw error;
        }
    }
    
    
    private int runTest(StressTestPluginApi api, int deviceId) throws Exception {
        Validate.notNull(api);
        
        int testId = 0;
        Exception error = null;
        
        // check status
        //StressTestSettings settings = getSettings(api, deviceId);
        //if (settings.getStatus() != StressTestTaskStatus.READYTORUN) {
        //    throw new StressTestException(Messages.StressTestPublisher_DeviceNotReady());
        //}
        
        // clone device
        for (int i = 0; i < RETRY_COUNT; ++i) {
            try {
                testId = api.cloneDevice(deviceId);
                error = null;
                break;
            }
            catch(Exception ex) {
                error = ex;
                Thread.sleep(RETRY_TIMEOUT);
                login(api);
            }
        }
        
        if (error != null) {
            throw error;
        }
        
        // run test
        for (int i = 0; i < RETRY_COUNT; ++i) {
            try {
                api.runTest(testId);
                return testId;  // Test has been started !!!
            }
            catch(Exception ex) {
                error = ex;
                Thread.sleep(RETRY_TIMEOUT);
                login(api);
            }
        }
        
        throw (error != null) ? 
            error : new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
    }

    
    private void login(StressTestPluginApi api) throws Exception {
        Validate.notNull(api);
        
        Exception error = null;
        for (int i = 0; i < RETRY_COUNT; ++i) {
            try {
                api.login();
                return;
            }
            catch(Exception ex) {
                error = ex;
                Thread.sleep(RETRY_TIMEOUT);
            }
        }
        
        throw (error != null) ? 
                error : new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
    }
    
    
    private StressTestSettings getSettings(StressTestPluginApi api, int deviceId) throws Exception {
        Validate.notNull(api);
        
        Exception error = null;
        for (int i = 0; i < RETRY_COUNT; ++i) {
            try {
                return api.getSettings(deviceId);
            }
            catch(Exception ex) {
                error = ex;
                Thread.sleep(RETRY_TIMEOUT);
                login(api);
            }
        }
        
        throw (error != null) ? 
                error : new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
    }
    
    
    private StressTestPluginData getResult(StressTestPluginApi api, int testId) throws Exception {
        Validate.notNull(api);
        
        Exception error = null;
        for (int i = 0; i < RETRY_COUNT; ++i) {
            try {
                return api.getResult(testId);
            }
            catch(Exception ex) {
                error = ex;
                Thread.sleep(RETRY_TIMEOUT);
                login(api);
            }
        }
        
        throw (error != null) ? 
                error : new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
    }
    
    
    private void abort(StressTestPluginApi api, int deviceId) {
        Validate.notNull(api);
        
        for (int i = 0; i < RETRY_COUNT; ++i) {
            try {
                api.abortTest(deviceId);
                return;
            }
            catch(Exception ex) {
                try
                {
                    Thread.sleep(RETRY_TIMEOUT);
                    login(api);
                }
                catch(Exception innerEx) {
                    // must be safe
                    return;
                }
            }
        }
    }
    
    
    private void printStatuses(StressTestTaskStatus status) {
        boolean found = false;
        List<StressTestTaskStatus> tempList = new ArrayList<>();
        
        for(StressTestTaskStatus provisionalStatus : provisionalStatuses) {
            tempList.add(provisionalStatus);
            found = (provisionalStatus == status);
            if (found)
                break;
        }

        if (found) {
            if (StressTestTaskStatus.CANCELLING == status) {
                provisionalStatuses.clear();
                printStatus(status);
            }
            else {
                for(StressTestTaskStatus printStatus : tempList) {
                    provisionalStatuses.remove(printStatus);
                    printStatus(printStatus);
                }
            }
        }
    }
    
    
    private void printStatus(StressTestTaskStatus status) {
        String message = getStatusDescription(status);
        message = String.format(Messages.StressTestPublisher_StressStatusTemplate(), message);
        console.println(message);
    }
    
    
    private String getStatusDescription(StressTestTaskStatus status) {
        if (StressTestTaskStatus.SCHEDULED == status)
            return Messages.StressTestPublisher_Scheduled();
        if (StressTestTaskStatus.SUBMITTING_TEST_REQUEST == status)
            return Messages.StressTestPublisher_SubmittingTestRequest();
        if (StressTestTaskStatus.INITIALIZING_TEST == status)
            return Messages.StressTestPublisher_InitializingTest();
        if (StressTestTaskStatus.STARTING_LOAD_INJECTORS == status)
            return Messages.StressTestPublisher_StartingLoadInjectors();
        if (StressTestTaskStatus.SENDING_EXECUTION_PLAN == status)
            return Messages.StressTestPublisher_SendingExecutionPlan();
        if (StressTestTaskStatus.RUNNING == status)
            return Messages.StressTestPublisher_Running();        
        if (StressTestTaskStatus.PREPARING_REPORT == status)
            return Messages.StressTestPublisher_PreparingReport();
        if (StressTestTaskStatus.CANCELLING == status)
            return Messages.StressTestPublisher_Cancelling();
        if (StressTestTaskStatus.CANCELLED == status)
            return Messages.StressTestPublisher_Cancelled();
        if (StressTestTaskStatus.FINISHED == status)
            return Messages.StressTestPublisher_Finished();
        else
            throw new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
    }
    
    
}
