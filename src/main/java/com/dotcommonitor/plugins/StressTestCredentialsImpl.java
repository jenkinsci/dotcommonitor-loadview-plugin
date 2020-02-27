package com.dotcommonitor.plugins;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONException;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 *
 * @author dotcom-monitor.com
 */
public class StressTestCredentialsImpl extends BaseStandardCredentials implements StressTestCredentials {

    private static final long serialVersionUID = 1L;
    private final Secret apiKey;
    
    
    @DataBoundConstructor
    public StressTestCredentialsImpl(CredentialsScope scope, String apiKey, String id, String description) {
        
        super(scope, id, description);
        this.apiKey = Secret.fromString(apiKey);
    }

    
    @Override
    public Secret getApiKey() {
        return apiKey;
    }
    
    
    @Extension
    public static class StressTestCredentialsDescriptor extends BaseStandardCredentialsDescriptor
    {
        
        @Override
        public String getDisplayName() {
            return Messages.StressTestCredentialsImpl_CredentialsDescription();
        }

        @POST
        public FormValidation doCheckApiKey(@QueryParameter String value) {
            return StringUtils.isBlank(value) ?
                FormValidation.error(Messages.StressTestCredentialsImpl_MandatoryField()) :
                FormValidation.ok();
        }
        
        @POST
        public FormValidation doTestConnection(@QueryParameter("apiKey") final Secret apiKey) 
                throws MessagingException, IOException, JSONException, ServletException {

            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            FormValidation result = null;
            
            try {
                StressTestPluginApi.checkLogin(apiKey.getPlainText());
                result = FormValidation.ok(Messages.StressTestCredentialsImpl_ValidApiKey());
            }
            catch (Exception ex) {
                String errorDescription = ex.getMessage();
                if (StringUtils.isBlank(errorDescription)) {
                    errorDescription = Messages.StressTestCredentialsImpl_InvalidApiKey();
                }
                result = FormValidation.error(errorDescription);
            }
            
            return result;
        }
        
        
    }
}
