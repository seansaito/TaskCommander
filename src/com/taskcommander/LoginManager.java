package com.taskcommander;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;

//@author A0112828H
/**
 * This class is used to connect to the Google API and
 * retrieve Google services after the user logs in through
 * the UI browser.
 */
public class LoginManager implements Observer {
	private static final String CLIENT_ID = "1009064713944-qqeb136ojidkjv4usaog806gcafu5dmn.apps.googleusercontent.com";
	private static final String CLIENT_SECRET = "9ILpkbnlGwVMQiqh10za3exf";
	private static final String APPLICATION_NAME = "Task Commander";

	private static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

	private static final String DATA_STORE_DIR = "credentials";
	private static final String DATA_STORE_NAME = "credentialDataStore";

	// Option to request access type for application. Can be "online" or "offline".
	private static final String FLOW_ACCESS_TYPE = "offline";
	// Option to request approval prompt type for application. Can be "force" or "auto".
	private static final String FLOW_APPROVAL_PROMPT = "auto";

	private static final String USERNAME = "User";
	
	private static LoginManager instance;

	private static FileDataStoreFactory dataStoreFactory;

	private HttpTransport httpTransport;
	private JsonFactory jsonFactory;
	private GoogleAuthorizationCodeFlow flow;
	private DataStore<StoredCredential> dataStore;
	private GoogleCredential credential;
	
	private static Logger logger = Logger.getLogger(LoginManager.class.getName());
	
    /**
     * Returns the only instance of LoginManager.
     * 
     * @return LoginManager instance
     */
	public static LoginManager getInstance() {
		if (instance == null) {
			instance = new LoginManager();
			instance.login();
		}
		return instance;
	}
	
	/**
	 * Returns a LoginManager instance and attempts to login.
	 */
	private LoginManager() {
		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();
		File dataStoreFile = new File(DATA_STORE_DIR);
		try {
			logger.log(Level.INFO,"Retrieving DataStore");
			dataStoreFactory = new FileDataStoreFactory(dataStoreFile);
			dataStore = dataStoreFactory.getDataStore(DATA_STORE_NAME);
		} catch (IOException e) {
			logger.log(Level.WARNING,"IOException: Unable to retrieve DataStore", e);
		}
	}

	// Service getter methods
	/**
	 * Connects to Google and builds a new Tasks service.
	 * Requests can be sent once this method is successfully
	 * executed.
	 */
	public Tasks getTasksService(){
		if (isLoggedIn()) {
			logger.log(Level.INFO,"Retrieved Tasks service");
			return new Tasks.Builder(httpTransport, jsonFactory, credential)
			.setApplicationName(APPLICATION_NAME).build();
		} else {
			return null;
		}
	}

	/**
	 * Connects to Google and builds a new Calendar service.
	 * Requests can be sent once this method is successfully
	 * executed.
	 */
	public Calendar getCalendarService(){
		if (isLoggedIn()) {
			logger.log(Level.INFO,"Retrieved Calendar service");
			return new Calendar.Builder(httpTransport, jsonFactory, credential)
			.setApplicationName(APPLICATION_NAME).build();
		} else {
			return null;
		}
	}
	
	// Data store related methods
	/**
	 * Gets the datastore factory used
	 * @return			dataStoreFactory
	 */
	public static DataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}
	
	// Login and credential methods
	/**
	 * Attempts to login.
	 */
	private void login() {
		if (credential == null) {
			credential = getCredential();
		}
	}
	
	/**
	 * Checks if logged in.
	 */
	private boolean isLoggedIn() {
		return credential != null;
	}

	/**
	 * Gets a GoogleCredential for use in Google API requests,
	 * either from storage or by sending a request to Google.
	 * @return           Credential
	 */
	private GoogleCredential getCredential() {
		logger.log(Level.INFO,"Attempting to get credentials.");
		if(hasStoredCredential()) {
			logger.log(Level.INFO,"Using stored credential.");
			setTokensFromStoredCredential();
			saveCredential();
		} else {
			logger.log(Level.INFO,"Getting new credential from login.");
			setTokensFromLogin();
		}
		return credential;
	}
	
	/**
	 * Checks if a credential with the given username has been stored in the 
	 * data store directory.
	 * @return If stored credential exists.
	 */
	private boolean hasStoredCredential() {
		try {
			return !dataStore.isEmpty() && dataStore.containsKey(USERNAME);
		} catch (IOException e) {
			logger.log(Level.WARNING,"IOException: Unable to check if DataStore contains key.", e);
			return false;
		}
	}
	
	//Helper methods
	/**
	 * Builds a GoogleCredential.
	 * @return
	 */
	private GoogleCredential buildCredential() {
		logger.log(Level.INFO,"Building credential.");
		GoogleCredential newCredential = new GoogleCredential.Builder()
		.setJsonFactory(jsonFactory)
		.setTransport(httpTransport)
		.setClientSecrets(CLIENT_ID, CLIENT_SECRET)
		.addRefreshListener(new DataStoreCredentialRefreshListener(USERNAME, dataStore))
		.build();
		return newCredential;
	}

	/**
	 * Saves the local credential in the datastore.
	 */
	private void saveCredential(){
		logger.log(Level.INFO, "Saving credential.");
		StoredCredential storedCredential = new StoredCredential();
		storedCredential.setAccessToken(credential.getAccessToken());
		storedCredential.setRefreshToken(credential.getRefreshToken());
		try {
			dataStore.set(USERNAME, storedCredential);
		} catch (IOException e) {
			logger.log(Level.WARNING,"IOException: Unable to store credential in DataStore.", e);
		}
	}

	// Stored credential methods
	/**
	 * Gets stored credential from data store and sets the tokens 
	 * in the local credential.
	 * @param newCredential 
	 */
	private void setTokensFromStoredCredential() {
		StoredCredential storedCredential;
		try {
			storedCredential = dataStore.get(USERNAME);
			GoogleCredential newCredential = buildCredential();
			newCredential.setAccessToken(storedCredential.getAccessToken());
			newCredential.setRefreshToken(storedCredential.getRefreshToken());
			credential = newCredential;
		} catch (IOException e) {
			logger.log(Level.WARNING,"IOException: Unable to retrieve StoredCredential.", e);
		}
	}

	// User login methods
	/**
	 * Requests the user to login and requests authorisation
	 * tokens. Has to wait for user to login in the UI and 
	 * retrieve token.
	 * @param newCredential 
	 */
	private void setTokensFromLogin() {
		requestAuthorisation();
	}

	/**
	 * Makes an authorisation request to Google.
	 */
	private void requestAuthorisation() {
		try {
			flow = buildAuthorisationCodeFlow();
		} catch (IOException e) {
			logger.log(Level.WARNING,"IOException: Unable to build authorisation code flow.", e);
		}
		
		getAuthorisationCode();
	}

	/**
	 * Creates the authorisation code flow needed for the authorisation URL.
	 * @return               GoogleAuthorizationCodeFlow object
	 * @throws IOException
	 */
	private GoogleAuthorizationCodeFlow buildAuthorisationCodeFlow() throws IOException {
		return new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(TasksScopes.TASKS, CalendarScopes.CALENDAR))
		.setAccessType(FLOW_ACCESS_TYPE)
		.setApprovalPrompt(FLOW_APPROVAL_PROMPT)
		.setDataStoreFactory(dataStoreFactory).build();
	}

	/**
	 * Creates the authorisation URL and passes it to the UI.
	 */
	private void getAuthorisationCode() {
		logger.log(Level.INFO, "Request user to login.");
		String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
		TaskCommander.ui.addObserver(LoginManager.getInstance());
		TaskCommander.ui.getCodeFromUser(url);
	}

	/**
	 * Once updated with the authorisation code, tries to get a token response
	 * using the code and sets a new credential from the token response.
	 */
	@Override
	public void update(Observable obs, Object obj) {
		logger.log(Level.INFO, "Login Manager updated, setting credential...");
		GoogleCredential newCredential = buildCredential();
		newCredential.setFromTokenResponse(getTokenResponse((String) obj));
		credential = newCredential;
		saveCredential();
	}
	
	/**
	 * Sends a token request to get a GoogleTokenResponse.
	 * If an IOException occurs, returns null.
	 * 
	 * @param code
	 * @return      Token response
	 */
	private GoogleTokenResponse getTokenResponse(String code) {
		logger.log(Level.INFO, "Get token response from Google with code "+code);
		try {
			GoogleTokenResponse response = flow.newTokenRequest(code)
					.setRedirectUri(REDIRECT_URI).execute();
			return response;
		} catch (IOException e) {
			logger.log(Level.WARNING,"IOException: Unable to execute GoogleTokenResponse", e);
		}
		return null;
	}

}
