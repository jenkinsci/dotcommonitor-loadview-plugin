package com.dotcommonitor.plugins;

import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 *
 * @author dotcom-monitor.com
 */
public class StressTestPluginApi {
    
    private final static String TAG_UID = "UID";
    private final static String TAG_ID = "ID";
    private final static String TAG_USER_NAME = "UserName";
    private final static String TAG_STARTING_EMAIL = "StartingEmail";
    private final static String TAG_ERROR_DESCRIPTION = "ErrorDescription";
    private final static String USER_NAME_JENKINS = "Jenkins";
    private final static int CONNECTION_TIMEOUT = 10 * 60 * 1000; // 10 min.
    private final StressTestUrlResolver urlResolver;
    private final String apiId;
    private final CloseableHttpClient httpClient;
    private final CookieStore cookieStore;
    
    
    public StressTestPluginApi(String uid, StressTestUrlResolver resolver) {
        this(uid, resolver, new BasicCookieStore());
    }

    
    public static void checkLogin(String uid) throws Exception {
        
        StressTestPluginApi api = null;
        
        try {
            StressTestUrlResolver resolver = new StressTestUrlResolver();
            api = new StressTestPluginApi(uid, resolver, null);
            api.login();
        }
        catch (Exception ex) {
            throw ex;
        }
        finally {
            if (api != null) {
                api.closeHttpClient();
            }
        }
    }
    
    
    public void login() throws Exception {
        
        if (cookieStore != null)
            cookieStore.clear();
        
        CloseableHttpResponse response = null;
        
        try {
            JSONObject json = new JSONObject();
            json.put(TAG_UID, apiId);
            String jsonText = json.toString();
            StringEntity requestEntity = new StringEntity(jsonText, ContentType.APPLICATION_JSON);
            String url = urlResolver.getUrlPostLogin();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(requestEntity);
            httpPost.setConfig(getConfig());
            
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(cookieStore);
            
            response = httpClient.execute(httpPost, context);
            
            StatusLine statusLine = (response != null) ? response.getStatusLine() : null;
            if (statusLine == null) {
                throw new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
            }
                
            int code = statusLine.getStatusCode();
            String responseText = getBody(response);
            String errorDescription = getErrorDescription(responseText);
            
            if (code >= HttpStatus.SC_MULTIPLE_CHOICES || code < HttpStatus.SC_OK) {
                if (StringUtils.isBlank(errorDescription)) {
                    errorDescription = Messages.StressTestPublisher_UnexpectedError();
                }
                String message = String.format(Messages.StressTestPublisher_RejectTemplate(), statusLine.toString(), errorDescription);
                throw new StressTestException(message);
            }
        }
        catch (Exception ex) {
            throw ex;
        }
        finally {
            safeCloseResponse(response);
        }
    }
            
    
    public StressTestSettings getSettings(int deviceId) throws Exception {
        
        String url = urlResolver.getUrlGetStatus(deviceId);
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        
        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(getConfig());
        
        try (CloseableHttpResponse response = httpClient.execute(httpGet, context)) {

            StatusLine statusLine = (response != null) ? response.getStatusLine() : null;
            if (statusLine == null) {
                throw new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
            }
                
            int code = statusLine.getStatusCode();
            String responseText = getBody(response);
            String errorDescription = getErrorDescription(responseText);
            
            if (code >= HttpStatus.SC_MULTIPLE_CHOICES || code < HttpStatus.SC_OK) {
                if (StringUtils.isBlank(errorDescription)) {
                    errorDescription = Messages.StressTestPublisher_UnexpectedError();
                }
                String message = String.format(Messages.StressTestPublisher_RejectTemplate(), statusLine.toString(), errorDescription);
                throw new StressTestException(message);
            }
            
            if (StringUtils.isBlank(responseText)) {
                throw new RuntimeException(Messages.StressTestPublisher_ResponseBodyEmpty());
            }
            
            StressTestSettings result = new StressTestSettings(responseText);
            return result;    
        }
        catch(Exception ex) {
            throw ex;
        } 
    }

    
    public int cloneDevice(int deviceId) throws Exception {

        String url = urlResolver.getUrlGetClone(deviceId);
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        
        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(getConfig());
        
        try (CloseableHttpResponse response = httpClient.execute(httpGet, context)) {
            
            StatusLine statusLine = (response != null) ? response.getStatusLine() : null;
            if (statusLine == null) {
                throw new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
            }
            
            int code = statusLine.getStatusCode();
            String responseText = getBody(response);
            String errorDescription = getErrorDescription(responseText);
            
            if (code >= HttpStatus.SC_MULTIPLE_CHOICES || code < HttpStatus.SC_OK) {
                if (StringUtils.isBlank(errorDescription)) {
                    errorDescription = Messages.StressTestPublisher_UnexpectedError();
                }
                String message = String.format(Messages.StressTestPublisher_RejectTemplate(), statusLine.toString(), errorDescription);
                throw new StressTestException(message);
            }
            
            if (StringUtils.isBlank(responseText)) {
                throw new RuntimeException(Messages.StressTestPublisher_ResponseBodyEmpty());
            }
                
            JSONObject json = new JSONObject(responseText);
            int testId = json.getInt(TAG_ID);
            return testId;
        }
        catch(Exception ex) {
            throw ex;
        }
    }

    
    public void runTest(int testId) throws Exception {
        
        String url = urlResolver.getUrlPostRun(testId);
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        
        CloseableHttpResponse response = null;
        
        try {
            JSONObject json = new JSONObject();
            json.put(TAG_USER_NAME, USER_NAME_JENKINS);
            json.put(TAG_STARTING_EMAIL, "");
            String jsonText = json.toString();
            StringEntity requestEntity = new StringEntity(jsonText, ContentType.APPLICATION_JSON);
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(requestEntity);
            httpPost.setConfig(getConfig());
            
            response = httpClient.execute(httpPost, context);
            
            StatusLine statusLine = (response != null) ? response.getStatusLine() : null;
            if (statusLine == null) {
                throw new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
            }
            
            int code = statusLine.getStatusCode();
            String responseText = getBody(response);
            String errorDescription = getErrorDescription(responseText);
            
            if (code >= HttpStatus.SC_MULTIPLE_CHOICES || code < HttpStatus.SC_OK) {
                if (StringUtils.isBlank(errorDescription)) {
                    errorDescription = Messages.StressTestPublisher_UnexpectedError();
                }
                String message = String.format(Messages.StressTestPublisher_RejectTemplate(), statusLine.toString(), errorDescription);
                throw new StressTestException(message);
            }
        }
        catch(Exception ex) {
            throw ex;
        }
        finally {
            safeCloseResponse(response);
        }
    }
    
    
    public StressTestPluginData getResult(int testId)  throws Exception {
    
        String url = urlResolver.getUrlGetResult(testId);
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        
        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(getConfig());
        
        try (CloseableHttpResponse response = httpClient.execute(httpGet, context)) {

            StatusLine statusLine = (response != null) ? response.getStatusLine() : null;
            if (statusLine == null) {
                throw new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
            }
                
            int code = statusLine.getStatusCode();
            String responseText = getBody(response);
            String errorDescription = getErrorDescription(responseText);
            
            if (code >= HttpStatus.SC_MULTIPLE_CHOICES || code < HttpStatus.SC_OK) {
                if (StringUtils.isBlank(errorDescription)) {
                    errorDescription = Messages.StressTestPublisher_UnexpectedError();
                }
                String message = String.format(Messages.StressTestPublisher_RejectTemplate(), statusLine.toString(), errorDescription);
                throw new StressTestException(message);
            }
            
            if (StringUtils.isBlank(responseText)) {
                throw new RuntimeException(Messages.StressTestPublisher_ResponseBodyEmpty());
            }
            
            StressTestPluginData result = new StressTestPluginData(responseText);
            return result;
        }
        catch(Exception ex) {
            throw ex;
        } 
    }
    
    
    public void abortTest(int deviceId) throws Exception {
        
        String url = urlResolver.getUrlGetAbort(deviceId);
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        
        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(getConfig());
        
        try (CloseableHttpResponse response = httpClient.execute(httpGet, context)) {

            StatusLine statusLine = (response != null) ? response.getStatusLine() : null;
            if (statusLine == null) {
                throw new RuntimeException(Messages.StressTestPublisher_UnexpectedError());
            }
                
            int code = statusLine.getStatusCode();
            if (code >= HttpStatus.SC_MULTIPLE_CHOICES || code < HttpStatus.SC_OK) {
                throw new StressTestException(Messages.StressTestPublisher_UnexpectedError());
            }
        }
        catch(Exception ex) {
            throw ex;
        } 
    }
    
    
    public void closeHttpClient() {
        try {
            httpClient.close();
        }
        catch(Exception ex) {
            // must be safe
        }
    }
    
    
    
    private StressTestPluginApi(String uid, StressTestUrlResolver resolver, CookieStore cookies) {
        Validate.notEmpty(uid);
        Validate.notNull(resolver);
        
        apiId = uid;
        httpClient = HttpClients.createDefault();
        cookieStore = cookies;
        urlResolver = resolver;
    }
    
    
    private String getBody(CloseableHttpResponse response) throws IOException {
        
        String responseText = "";
        if (response != null) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                responseText = EntityUtils.toString(responseEntity);
            }
        }
        
        return responseText;
    }
    
    
    private String getErrorDescription(String body) {
        
        if (StringUtils.isBlank(body))
            return body;
        
        StringBuilder result = new StringBuilder();
        try {
            JSONObject json = new JSONObject(body);
            if (json.has(TAG_ERROR_DESCRIPTION)) {
                JSONArray array = json.getJSONArray(TAG_ERROR_DESCRIPTION);
                if (array != null) {
                    for (Object item : array) { 
                        if (item != null) {
                            if (result.length() > 0) {
                                result.append("; ");
                            }
                            result.append(item.toString());
                        }
                    }
                }
            }
        }
        catch(Exception ex) {
            return body;
        }
        
        return result.length() > 0 ? result.toString() : body;
    }
    
    
    private void safeCloseResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            }
            catch(Exception ex) {
                // must be safe
            }
        }
    }
    
    
    private RequestConfig getConfig() {
        RequestConfig.Builder configBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
        configBuilder.setSocketTimeout(CONNECTION_TIMEOUT);
        return configBuilder.build();
    }
    
}
