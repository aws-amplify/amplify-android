package com.amplifyframework.api.aws;

import android.util.Log;

import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amplifyframework.api.aws.sigv4.AppSyncV4Signer;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

class SubscriptionAuthorizationUtility {

    private static final String TAG = SubscriptionAuthorizationUtility.class.getSimpleName();

    /**
     * Return authorization json to be used for connection and subscription registration.
     *
     * @return
     */
    protected static JSONObject getAuthorizationDetails(boolean connectionFlag, ApiConfiguration apiConfiguration, String gqlDocument, Map<String, String> variable) throws JSONException {
        JSONObject authorizationMessage = new JSONObject();
        // Get the Auth Mode from configuration json
        String authMode = null;
        authMode = apiConfiguration.getAuthorizationType().toString();

        // Construct the Json based on the Auth Mode
        switch (authMode) {
            case "API_KEY" :
                authorizationMessage = getAuthorizationDetailsForApiKey(apiConfiguration);
                break;
            case "AWS_IAM" :
                authorizationMessage = getAuthorizationDetailsForIAM(connectionFlag, apiConfiguration, gqlDocument, variable);
                break;
            case "AMAZON_COGNITO_USER_POOLS" :
                authorizationMessage = getAuthorizationDetailsForUserpools(apiConfiguration);
                break;
            case "OPENID_CONNECT" :
                authorizationMessage = getAuthorizationDetailsForOidc(apiConfiguration);
                break;
            default :
                Log.e(TAG, "Unsupported Auth type detected");
                break;
        }

        return authorizationMessage;
    }

    private static JSONObject getAuthorizationDetailsForApiKey(ApiConfiguration apiConfiguration) {
        JSONObject authorizationMessage = new JSONObject();
        try {
            authorizationMessage.put("host",
                    getHost(apiConfiguration.getEndpoint()));
            authorizationMessage.put("x-amz-date",
                    ISO8601Timestamp.now());
            authorizationMessage.put("x-api-key",
                    apiConfiguration.getApiKey());
        } catch (JSONException | MalformedURLException e) {
            Log.e(TAG, "Error constructing the authorization json", e);
        }
        return authorizationMessage;
    }

    private static JSONObject getAuthorizationDetailsForIAM(boolean connectionFlag,
                                                     ApiConfiguration apiConfiguration,
                                                     String queryDocument,
                                                     Map<String, String> variable) throws JSONException {
        JSONObject authorizationMessage = new JSONObject();
        String regionStr = null;

        DefaultRequest<?> canonicalRequest = new DefaultRequest<>("appsync");
        try {
            if (connectionFlag) {
                canonicalRequest.setEndpoint(new URI(apiConfiguration.getEndpoint() + "/connect"));
            } else {
                canonicalRequest.setEndpoint(new URI(apiConfiguration.getEndpoint()));
            }
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error constructing canonical URI for IAM request signature", e);
        }
        canonicalRequest.addHeader("accept", "application/json, text/javascript");
        canonicalRequest.addHeader("content-encoding", "amz-1.0");
        canonicalRequest.addHeader("content-type", "application/json; charset=UTF-8");

        canonicalRequest.setHttpMethod(HttpMethodName.valueOf("POST"));
        if (connectionFlag) {
            canonicalRequest.setContent(new ByteArrayInputStream("{}".getBytes()));
        } else {
            canonicalRequest.setContent(new ByteArrayInputStream(getDataJson(queryDocument, variable).getBytes()));
        }
        if (connectionFlag){
            new AppSyncV4Signer(regionStr, AppSyncV4Signer.ResourcePath.IAM_CONNECTION_RESOURCE_PATH)
                    .sign(canonicalRequest, AWSMobileClient.getInstance().getCredentials());
        } else {
            new AppSyncV4Signer(regionStr).sign(canonicalRequest,
                    AWSMobileClient.getInstance().getCredentials());
        }

        Map<?, ?> signedHeaders = canonicalRequest.getHeaders();
        try {
            for(Map.Entry<?, ?> headerEntry : signedHeaders.entrySet()) {
                if (! headerEntry.getKey().equals("host")) {
                    authorizationMessage.put((String) headerEntry.getKey(), headerEntry.getValue());
                } else {
                    authorizationMessage.put("host", getHost(apiConfiguration.getEndpoint()));
                }
            }
        } catch (JSONException | MalformedURLException e) {
            Log.e(TAG, "Error constructing authorization message json", e);
        }
        return authorizationMessage;
    }

    private static JSONObject getAuthorizationDetailsForUserpools(ApiConfiguration apiConfiguration) {
        JSONObject authorizationMessage = new JSONObject();

        CognitoUserPoolsAuthProvider authProvider = new DefaultCognitoUserPoolAuthProvider();
        String token = authProvider.getLatestAuthToken();
        try {
            authorizationMessage.put("host", getHost(apiConfiguration.getEndpoint()));
            authorizationMessage.put("Authorization", token);
        } catch (JSONException | MalformedURLException e) {
            Log.e(TAG, "Error constructing authorization message json", e);
        }

        return authorizationMessage;
    }

    private static JSONObject getAuthorizationDetailsForOidc(ApiConfiguration apiConfiguration) {
        JSONObject authorizationMessage = new JSONObject();
        try {
            OidcAuthProvider oidcAuthProvider = ApiAuthProviders.noProviderOverrides().getOidcAuthProvider();
            String token = oidcAuthProvider.getLatestAuthToken();
            authorizationMessage.put("host", getHost(apiConfiguration.getEndpoint()));
            authorizationMessage.put("Authorization", token);
        } catch (JSONException | MalformedURLException e) {
            Log.e(TAG, "Error constructing authorization message json", e);
        }
        return authorizationMessage;
    }

    private static String getHost(String apiUrl) throws MalformedURLException {
        return new URL(apiUrl).getHost();
    }

    private static String getDataJson(String queryDcoument, Map<String, String> variables) {
        String query = queryDcoument;
        JSONObject dataJson = new JSONObject();
        try {
            dataJson.put("query", query);
            JSONObject variableJson = new JSONObject();
            for (Map.Entry<String, String> e : variables.entrySet()){
                variableJson.put(e.getKey(), e.getValue());
            }
            dataJson.put("variables", variableJson);
        } catch (JSONException e) {
            Log.e(TAG, "Error constructing JSON object", e);
        }

        return dataJson.toString();
    }

    /**
     * Utility to create a ISO 8601 compliant timestamps
     */
    public static final class ISO8601Timestamp {
        public static String now() {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            return formatter.format(new Date());
        }
    }
}
