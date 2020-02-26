package com.dotcommonitor.plugins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;


/**
 *
 * @author dotcom-monitor.com
 */
public class StressTestPublisher extends Notifier implements SimpleBuildStep {
    
    private final String scenarioId;
    private final String credentialsId;
    private final int errorThreshold;
    private final int avgTime;
    
    
    @DataBoundConstructor
    public StressTestPublisher(String scenarioId, String credentialsId, int errorThreshold, int avgTime) {
        Validate.notEmpty(scenarioId);
        Validate.notEmpty(credentialsId);
        
        this.scenarioId = scenarioId;
        this.credentialsId = credentialsId;
        this.errorThreshold = errorThreshold;
        this.avgTime = avgTime;
    }
    
    
    public String getScenarioId() {
        return scenarioId;
    }

    
    public String getCredentialsId() {
        return credentialsId;
    }
            
    
    public int getErrorThreshold() {
        return errorThreshold;
    }
    
    
    public int getAvgTime() {
        return avgTime;
    }
    
    
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    
    @Override
    public void perform(Run<?, ?> run, FilePath fp, Launcher lnchr, TaskListener listener) 
            throws InterruptedException, IOException {
        
        Boolean result = false;
        StressTestProcessor processor;
        PrintStream console = listener.getLogger();
                
        try {
            processor = new StressTestProcessor(console, run, this);
            result = processor.process();
        }
        catch (StressTestException ex) {
            console.println(ex.getMessage());
        }
        catch (InterruptedException ex) {
            console.println(Messages.StressTestPublisher_BuildCancelled());
        }
        catch (Exception ex) {
            String message = StringUtils.isBlank(ex.getMessage()) ? Messages.StressTestPublisher_UnexpectedError() : ex.getMessage();
            console.println(message);
            ex.printStackTrace(console);
        }
        
        if (!result)
            run.setResult(Result.FAILURE);
    }
    
    
    @Symbol("dotcomMonitor")
    @Extension
    public static class StressTestPublisherDescriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> type) {
            return true;
        }
        
        
        @Override
	public String getDisplayName() {
            return Messages.StressTestPublisher_DisplayName();
	}

        @POST
        public FormValidation doCheckScenarioId(@QueryParameter String value) {
            
            return StringUtils.isBlank(value) ?
                FormValidation.error(Messages.StressTestCredentialsImpl_MandatoryField()) :
                FormValidation.ok();
        }
        
        @POST
        public FormValidation doCheckCredentialsId(final @AncestorInPath Item item, @QueryParameter String value) {
            
            if (item == null) {
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } 
            else {
                if (!item.hasPermission(Item.EXTENDED_READ)&& !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            } 
            
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.StressTestCredentialsImpl_MandatoryField());
            } 
            
            if (CredentialsProvider.listCredentials(StressTestCredentials.class, item, ACL.SYSTEM, 
                    Collections.<DomainRequirement>emptyList(), CredentialsMatchers.withId(value)).isEmpty()) {
                return FormValidation.error(Messages.StressTestPublisher_InvalidCredentials());
            }
            
            return FormValidation.ok();
        }
        
        @POST
        public FormValidation doCheckAvgTime(@QueryParameter String value) {
            
            FormValidation result;
            
            try {  
                int temp = Integer.parseInt(value);  
                result = (temp > 0) ? FormValidation.ok() : FormValidation.error(Messages.StressTestPublisher_ZeroIntegerValue());
            } catch (NumberFormatException ex) {  
                result = FormValidation.error(Messages.StressTestPublisher_InvalidIntegerValue());
            }   
            
            return result;
        }
        
        @POST
        public FormValidation doCheckErrorThreshold(@QueryParameter String value) {
            
            FormValidation result;
            
            try {  
                int temp = Integer.parseInt(value);
                result = (temp >= 0 && temp <= 100) ? 
                        FormValidation.ok() : 
                        FormValidation.error(Messages.StressTestPublisher_PercentIntegerValue());
            } catch (NumberFormatException ex) {  
                result = FormValidation.error(Messages.StressTestPublisher_InvalidIntegerValue());
            }   
            
            return result;
        }
        
        @POST
        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath Item item, @QueryParameter String credentialsId) {
            
            StandardListBoxModel result = new StandardListBoxModel();
            
            if (item == null) {
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) { 
                    result.includeEmptyValue();
                    result.includeCurrentValue(credentialsId);
                    return result;
                }
            }
            else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    result.includeEmptyValue();    
                    result.includeCurrentValue(credentialsId);
                    return result;
                }
            } 
            
            result.includeEmptyValue();
            result.includeMatchingAs(ACL.SYSTEM, item, StressTestCredentials.class, Collections.<DomainRequirement>emptyList(), CredentialsMatchers.always());
            result.includeCurrentValue(credentialsId);
            
            return result;
        }
        
    }

}
