package com.github.kdvolder.azure.experiment;

import java.io.IOException;
import java.util.List;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.ServiceResourceInner;
import com.microsoft.azure.management.resources.Subscription;

public class STSAzureClient {
	
	private static final String TOKEN_FILE = "/tmp/azure-tokens.json";

	/**
	 * Informations identifying a user and their credentials. These are not used directly
	 * to access the api. Instead they are used to obtain 'authTokens'.
	 */
	private AzureCredential credentials;
	
	/**
	 * Credentials are used to obtain oauthTokens:
	 */
	private AzureTokenCredentials authTokens;

    /**
     * The azure client for get list of subscriptions (i.e. this client is not targetted to
     * a specific spring cloud cluster (called a 'service' in Azure speak. This is the equivalent
     * of a CF space (i.e. a place in which apps can be deployed). So this client is 
     * authenticated to a specific user but not targeted to any particular subscription/group/service.
     */
    private Authenticated azure;
    
    protected SpringServiceClient springServiceClient;
    
    /**
     * Set to a specific subscription once it has been selected. Set to null otherwise.
     */
    private Subscription sub; 

	public void authenticate() throws Exception {
        getAuthTokens();
        this.azure = Azure.authenticate(authTokens);
        if (azure==null) {
        	throw new IOException("Couldn't obtain credentials");
        }
	}

	private void getAuthTokens() throws Exception {
		final AzureEnvironment environment = AzureEnvironment.AZURE;
//        try {
            this.credentials = AzureAuthHelper.oAuthLogin(environment);
            
         // We can't use the deviceLogin helper as is because it prints to sysout to interact with the user
         // and instructing them to get paste a auth code into a browser.
//        } catch (DesktopNotSupportedException e) {
//            this.credentials = AzureAuthHelper.deviceLogin(environment);
//        }
        authTokens = AzureAuthHelper.getMavenAzureLoginCredentials(credentials, environment);
	}
	
	public static void main(String[] args) throws Exception {
		STSAzureClient client = new STSAzureClient();
		client.connect();
	}

	public SpringServiceClient getSpringServiceClient() {
		if (springServiceClient == null) {
			springServiceClient = new SpringServiceClient(authTokens, sub.subscriptionId(), getUserAgent());
		}
		return springServiceClient;
	}

    private String getUserAgent() {
    	return "spring-tool-suite/4.1.5-testing";
	}

	private void selectAppCluster() throws Exception {
        final List<ServiceResourceInner> clusters = getSpringServiceClient().getAvailableClusters();
        for (ServiceResourceInner cluster : clusters) {
        	System.out.println(cluster.name() +" [type: "+cluster.type()+"]");
		}
    }

	public STSAzureClient connect() throws Exception {
		authenticate();
		selectSub();
		selectAppCluster();
		
		return this;
	}

	private void selectSub() {
		PagedList<Subscription> subs = azure.subscriptions().list();
		subs.forEach(sub -> {
			System.out.println("Subscription: "+sub.displayName());
			System.out.println("          Id: "+sub.subscriptionId());
			azure.withSubscription(sub.subscriptionId());
			System.out.println(sub);
		});
		//TODO:
//		if (subs.size()>1) {
//			 choose one
//		} else {
		this.sub = subs.get(0);
//	    }
	}

}
