package io.slingr.endpoints.googlecalendar;

import com.google.api.client.http.HttpResponseException;
import io.slingr.endpoints.PerUserEndpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.framework.annotations.*;
import io.slingr.endpoints.googlecalendar.services.*;
import io.slingr.endpoints.googlecalendar.services.entities.ValidToken;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.datastores.DataStore;
import io.slingr.endpoints.services.datastores.DataStoreResponse;
import io.slingr.endpoints.services.exchange.Parameter;
import io.slingr.endpoints.services.exchange.ReservedName;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.MapsUtils;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Google Calendar endpoint
 *
 * <p>Created by lefunes on 08/11/15.
 */
@SlingrEndpoint(name = "google-calendar")
public class GoogleCalendarEndpoint extends PerUserEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarEndpoint.class);

    // time constants
    private static final long MIN_POLLING_TIME = TimeUnit.MINUTES.toMillis(5);
    private static final long DEFAULT_POLLING_TIME = TimeUnit.MINUTES.toMillis(10);
    private static final long DEFAULT_STARTING_TIME = TimeUnit.SECONDS.toMillis(5);
    private static final long MAX_WAITING_BETWEEN_SYNCS = TimeUnit.DAYS.toMillis(15);

    // method parameters
    private static final String PARAMETER_CALENDAR_ID = "contactId";
    private static final String PARAMETER_EVENT_ID = "eventId";
    private static final String PARAMETER_ID = "_id";
    private static final String PARAMETER_LAST_SYNC = "last_sync";
    private static final String PARAMETER_CALENDARS = "calendars";
    private static final String PARAMETER_STATUS = "status";
    private static final String PARAMETER_STATUS_CANCELLED = "cancelled";
    private static final String OLD_FUNCTION_NAME = "__functionName";

    // user configuration properties
    private static final String PROPERTY_ID = "_id";
    private static final String PROPERTY_RESULT = "result";
    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_PICTURE = "picture";
    public static final String PROPERTY_CODE = "code";
    private static final String PROPERTY_LAST_CODE = "lastCode";
    private static final String PROPERTY_REDIRECT_URI = "redirectUri";
    private static final String PROPERTY_TOKEN = "token";
    private static final String PROPERTY_REFRESH_TOKEN = "refreshToken";
    private static final String PROPERTY_EXPIRATION_TIME = "expirationTime";
    private static final String PROPERTY_TIMEZONE = "timezone";
    private static final String PROPERTY_ERROR = "error";

    // sync tags
    private static final String ITEM_NAME = "event";
    private static final String ITEMS_NAME = "events";
    private static final String TAG_EVENT_NUM = "n_event";
    private static final String TAG_EVENTS = "events";
    private static final String TAG_LAST_SYNC = "last_sync";
    private static final String TAG_CALENDARS = "calendars";
    private static final String TAG_CALENDAR = "calendar";
    private static final String TAG_CALENDAR_ID = "calendar_id";
    private static final String TAG_TOKEN_LAST = "last_token";
    private static final String TAG_TOKEN_NEW = "new_token";
    private static final String TAG_SYNC = "sync";
    private static final String TAG_USERS = "users";
    private static final String TAG_USER = "user";
    private static final String TAG_USER_ID = "user_id";
    private static final String TAG_EVENTS_TOTAL = "total_events";
    private static final String TAG_EVENT = "event";

    // old methods
    private static final String OLD_METHOD_GET_CALENDARS = "getCalendars";
    private static final String OLD_METHOD_CREATE_CALENDAR = "createCalendar";
    private static final String OLD_METHOD_UPDATE_CALENDAR = "updateCalendar";
    private static final String OLD_METHOD_REMOVE_CALENDAR = "removeCalendar";
    private static final String OLD_METHOD_CLEAR_CALENDAR = "clearCalendar";
    private static final String OLD_METHOD_GET_EVENTS = "getEvents";
    private static final String OLD_METHOD_SYNC_EVENTS = "syncEvents";
    private static final String OLD_METHOD_CREATE_EVENT = "createEvent";
    private static final String OLD_METHOD_UPDATE_EVENT = "updateEvent";
    private static final String OLD_METHOD_REMOVE_EVENT = "removeEvent";

    // event names
    private static final String SYNC_EVENT_DELETED = "syncEventDeleted";
    private static final String SYNC_EVENT_UPDATED = "syncEventUpdated";

    @ApplicationLogger
    private AppLogs appLogs;

    @EndpointDataStore(name = "cal_sync")
    private DataStore pollingDataStore;

    @EndpointUserDataStore
    private DataStore usersDataStore;

    @EndpointProperty
    private String clientId;

    @EndpointProperty
    private String clientSecret;

    @EndpointProperty
    private String redirectUri;

    @EndpointProperty(name = "single")
    private String clientType;

    @EndpointProperty
    private String javascriptOrigin;

    @EndpointProperty
    private String syncTime;

    private String defaultRedirectUri = "";

    @EndpointProperty
    private String pollingEnabled;

    @EndpointConfiguration
    private Json configuration;

    private GoogleClient client = null;
    private final ReentrantLock userSyncLock = new ReentrantLock();
    private final AtomicLong pollingCounter = new AtomicLong(0);

    @Override
    public void endpointStarted() {
        clientType = clientType != null && Arrays.asList("single", "multi").contains(clientType.toLowerCase()) ? clientType.toLowerCase() : "single";
        this.defaultRedirectUri = (
                properties().isLocalDeployment() ? "http://" : "https://"+
                        ("multi".equalsIgnoreCase(clientType) ? "" : properties().getApplicationName()+".")
        )+properties().getBaseDomain()+"/callback";

        // google client
        client = new GoogleClient(properties().getApplicationName(), clientId, clientSecret, redirectUri, ServiceType.values());

        // polling
        if ("enable".equals(this.pollingEnabled)) {
            long syncTime = DEFAULT_POLLING_TIME;
            if(StringUtils.isNotBlank(this.syncTime)){
                try {
                    syncTime = TimeUnit.MINUTES.toMillis(Long.parseLong(this.syncTime));
                } catch (Exception ex){
                    logger.warn(String.format("Invalid configured polling time [%s]", syncTime));
                }
            }
            if(syncTime < MIN_POLLING_TIME){
                syncTime = MIN_POLLING_TIME;
            }

            Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(this::pollingProcess, DEFAULT_STARTING_TIME, syncTime, TimeUnit.MILLISECONDS);

            logger.info(String.format("Calendars polling enabled each [%s] ms", syncTime));
        } else {
            logger.info("Calendars polling disabled");
        }
    }

    @EndpointFunction(name = ReservedName.CONNECT_USER)
    public Json connectUsers(FunctionRequest request) {
        final String userId = request.getUserId();
        if(StringUtils.isBlank(userId)) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "User ID is required").returnCode(400);
        }

        final Json body = request.getJsonParams();
        final String functionId = request.getFunctionId();

        // default values
        final Json configuration = Json.map();
        configuration.set(PROPERTY_RESULT, "An error happened when connecting to Google. Please contact to administrator.");
        configuration.set(PROPERTY_NAME, null);
        configuration.set(PROPERTY_PICTURE, null);
        configuration.set(PROPERTY_CODE, null);
        configuration.set(PROPERTY_LAST_CODE, null);
        configuration.set(PROPERTY_REDIRECT_URI, null);
        configuration.set(PROPERTY_TOKEN, null);
        configuration.set(PROPERTY_REFRESH_TOKEN, null);
        configuration.set(PROPERTY_EXPIRATION_TIME, null);
        configuration.set(PROPERTY_TIMEZONE, null);

        boolean connected = false;
        try {
            // check stored configuration
            try {
                final Json storedConfiguration = usersDataStore.findById(userId);
                if (storedConfiguration != null) {
                    configuration.setIfNotNull(PROPERTY_RESULT, storedConfiguration.string(PROPERTY_RESULT));
                    configuration.setIfNotNull(PROPERTY_NAME, storedConfiguration.string(PROPERTY_NAME));
                    configuration.setIfNotNull(PROPERTY_PICTURE, storedConfiguration.string(PROPERTY_PICTURE));
                    configuration.setIfNotNull(PROPERTY_LAST_CODE, storedConfiguration.string(PROPERTY_LAST_CODE));
                    configuration.setIfNotNull(PROPERTY_TOKEN, storedConfiguration.string(PROPERTY_TOKEN));
                    configuration.setIfNotNull(PROPERTY_REFRESH_TOKEN, storedConfiguration.string(PROPERTY_REFRESH_TOKEN));
                    configuration.setIfNotNull(PROPERTY_EXPIRATION_TIME, storedConfiguration.string(PROPERTY_EXPIRATION_TIME));
                    configuration.setIfNotNull(PROPERTY_TIMEZONE, storedConfiguration.string(PROPERTY_TIMEZONE));
                }
            } catch (Exception ex){
                logger.info(String.format("User configuration not found [%s] [%s]", userId, ex.getMessage()), ex);
            }

            if (body != null) {
                // update new parameters
                configuration.setIfNotNull(PROPERTY_RESULT, body.string(PROPERTY_RESULT));
                configuration.setIfNotNull(PROPERTY_NAME, body.string(PROPERTY_NAME));
                configuration.setIfNotNull(PROPERTY_PICTURE, body.string(PROPERTY_PICTURE));
                configuration.setIfNotNull(PROPERTY_TOKEN, body.string(PROPERTY_TOKEN));
                configuration.setIfNotNull(PROPERTY_CODE, body.string(PROPERTY_CODE));
                configuration.setIfNotNull(PROPERTY_LAST_CODE, body.string(PROPERTY_LAST_CODE));
                configuration.setIfNotNull(PROPERTY_REDIRECT_URI, body.string(PROPERTY_REDIRECT_URI));
                configuration.setIfNotNull(PROPERTY_REFRESH_TOKEN, body.string(PROPERTY_REFRESH_TOKEN));
                configuration.setIfNotNull(PROPERTY_EXPIRATION_TIME, body.string(PROPERTY_EXPIRATION_TIME));
                configuration.setIfNotNull(PROPERTY_TIMEZONE, body.string(PROPERTY_TIMEZONE));
            }

            // generate token from code if code is present
            final String code = configuration.string(PROPERTY_CODE);
            configuration.set(PROPERTY_CODE, null);

            final String redirectUri = configuration.string(PROPERTY_REDIRECT_URI);
            configuration.set(PROPERTY_REDIRECT_URI, null);

            if (StringUtils.isNotBlank(code)) {
                if(!code.equals(configuration.string(PROPERTY_LAST_CODE))) {
                    final Json tokens = client.generateTokensFromCode(code, redirectUri);
                    if (tokens != null && StringUtils.isBlank(tokens.string(PROPERTY_ERROR))) {
                        configuration.set(PROPERTY_RESULT, "Connection established.");
                        configuration.set(PROPERTY_TOKEN, tokens.string(PROPERTY_TOKEN));
                        configuration.set(PROPERTY_REFRESH_TOKEN, tokens.string(PROPERTY_REFRESH_TOKEN));
                        configuration.set(PROPERTY_EXPIRATION_TIME, tokens.string(PROPERTY_EXPIRATION_TIME));
                        configuration.set(PROPERTY_LAST_CODE, code);
                    }
                }
            }

            Json checkedToken = client.checkTokenFromConfiguration(userId, configuration);
            if (StringUtils.isBlank(checkedToken.string(PROPERTY_ERROR))) {
                configuration.set(PROPERTY_TOKEN, checkedToken.string(PROPERTY_TOKEN));
                configuration.set(PROPERTY_REFRESH_TOKEN, checkedToken.string(PROPERTY_REFRESH_TOKEN));
                configuration.set(PROPERTY_EXPIRATION_TIME, checkedToken.string(PROPERTY_EXPIRATION_TIME));
            }

            if (StringUtils.isNotBlank(configuration.string(PROPERTY_TOKEN))) {
                connected = true;
                configuration.set(PROPERTY_RESULT, "Connection established.");

                final GoogleAuthenticationService service = client.getAuthenticationService(configuration.string(PROPERTY_TOKEN));
                if(service != null) {
                    Json user = service.getUserInformation();
                    if(user != null && StringUtils.isBlank(user.string(PROPERTY_TOKEN))){
                        configuration.set(PROPERTY_RESULT, "Connection established as " + user.string(PROPERTY_NAME) + ".");
                        configuration.set(PROPERTY_NAME, user.string(PROPERTY_NAME));
                        configuration.set(PROPERTY_PICTURE, user.string(PROPERTY_PICTURE));
                    }
                }
            }

        } catch (Exception ex){
            final String connectionError = String.format("Error when try to connect user [%s] [%s]", userId, ex.getMessage());
            logger.warn(connectionError, ex);
            appLogs.error(connectionError);
        }

        configuration.set("_id", userId);
        final Json conf = usersDataStore.save(configuration);
        if(connected) {
            final Json event = Json.map()
                    .setIfNotNull("userId", userId)
                    .setIfNotNull("userEmail", request.getUserEmail());

            if (conf != null) {
                logger.info(String.format("User connected [%s] [%s]", conf.string(PROPERTY_ID), conf.toString()));
                event.set("configuration", conf);
            } else {
                configuration.set("_id", userId);
                logger.info(String.format("An error happened when tries to save the new user configuration [%s] [%s]", userId, configuration.toString()));
                event.set("configuration", configuration);
            }

            // sends connected user event
            users().sendUserConnectedEvent(functionId, userId, event);

            return event;
        }

        logger.info(String.format("User [%s] can not be connected to Google", userId));
        return disconnectUser(userId, request.getUserEmail(), functionId, true);
    }

    @EndpointFunction(name = ReservedName.DISCONNECT_USER)
    public Json disconnectUser(FunctionRequest request) {
        final String userId = request.getUserId();
        if(StringUtils.isBlank(userId)) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "User ID is required").returnCode(400);
        }

        final String functionId = request.getFunctionId();

        return disconnectUser(userId, request.getUserEmail(), functionId, true);
    }

    /**
     * Disconnect user and invalidate the token
     *
     * @param userId user id
     * @param functionId function id
     * @param revokeToken true if the token must be invalidated
     * @return last user configuration
     */
    public Json disconnectUser(final String userId, final String userEmail, final String functionId, boolean revokeToken){
        logger.info(String.format("Disconnect user [%s] request", userId));

        // default values
        final Json configuration = Json.map();
        configuration.set(PROPERTY_RESULT, "Connection disabled.");
        configuration.set(PROPERTY_NAME, null);
        configuration.set(PROPERTY_PICTURE, null);
        configuration.set(PROPERTY_CODE, null);
        configuration.set(PROPERTY_LAST_CODE, null);
        configuration.set(PROPERTY_TOKEN, null);
        configuration.set(PROPERTY_REFRESH_TOKEN, null);
        configuration.set(PROPERTY_EXPIRATION_TIME, null);
        configuration.set(PROPERTY_TIMEZONE, null);

        if(StringUtils.isNotBlank(userId)) {
            // revoke tokens
            if(revokeToken) {
                final Json storedConfiguration = getUserConfiguration(userId);
                if (storedConfiguration != null && !storedConfiguration.isEmpty()) {
                    client.revokeTokens(storedConfiguration.string(PROPERTY_TOKEN), storedConfiguration.string(PROPERTY_REFRESH_TOKEN));
                    logger.info(String.format("Revoked tokens for user [%s]", userId));
                }
            }
            try {
                final Object response = events().sendSync(ReservedName.USER_DISCONNECTED, null, functionId, userId, null);
                if (response != null) {
                    removeUserConfiguration(userId);
                } else {
                    logger.warn(String.format("The event to disconnect the user fail [%s]", userId));
                }
            } catch(Exception ex){
                logger.warn(String.format("The event to disconnect the user fail [%s]: %s", userId, ex.toString()));
            }
        }

        // sends disconnected user event
        users().sendUserDisconnectedEvent(functionId, userId);

        return Json.map()
                .setIfNotNull("configuration", configuration)
                .setIfNotNull("userId", userId)
                .setIfNotNull("userEmail", userEmail);
    }

    public Json saveUserConfiguration(String userId, Json newConfiguration){
        return saveUserConfiguration(userId, newConfiguration, true);
    }

    public Json saveUserConfiguration(String userId, Json newConfiguration, boolean getConfiguration){
        if(StringUtils.isNotBlank(userId)){
            logger.debug(String.format("Save user configuration [%s]", userId));
            try {
                // check last user configuration
                Json user = getConfiguration ? getUserConfiguration(userId) : null;
                if (user == null) {
                    user = Json.map();
                }

                // check new user configuration
                if (newConfiguration != null) {
                    user.merge(newConfiguration);
                }

                // save configuration
                user.set("_id", userId);
                usersDataStore.save(user);

                logger.debug(String.format("User configuration [%s] was saved [%s]", userId, user.toString()));

                return user;
            } catch (Exception ex){
                logger.warn(String.format("Error when try to save user configuration [%s] [%s]", userId, ex.getMessage()), ex);
            }
        } else {
            logger.warn(String.format("User id is empty [%s]", userId));
        }
        return null;
    }

    public void removeUserConfiguration(String userId){
        if(StringUtils.isNotBlank(userId)){
            logger.debug(String.format("Remove user configuration [%s]", userId));
            try {
                // remove last user configuration
                usersDataStore.removeById(userId);

                logger.debug(String.format("User configuration [%s] was deleted", userId));
            } catch (Exception ex){
                logger.warn(String.format("Error when try to delete user configuration [%s] [%s]", userId, ex.getMessage()), ex);
            }
        }
    }

    public Json getUserConfiguration(String userId){
        Json response = null;
        if(StringUtils.isNotBlank(userId)){
            logger.debug(String.format("Checking user configuration [%s]", userId));
            try {
                // check last user configuration
                response = usersDataStore.findById(userId);

                if(response != null && !response.isEmpty()) {
                    logger.info(String.format("User configuration [%s] was found", userId));
                } else {
                    logger.info(String.format("User configuration [%s] was not found", userId));
                }
            } catch (Exception ex){
                logger.warn(String.format("Error when try to find user configuration [%s] [%s]", userId, ex.getMessage()), ex);
            }
        }
        return response;
    }

    public void checkDisconnection(final String userId, final HttpResponseException httpException, final String functionId){
        final StringBuilder err = new StringBuilder();
        err.append(httpException.getStatusCode()).append(" ");
        if(httpException.getStatusMessage() != null){
            err.append(" - ").append(httpException.getStatusMessage());
        }
        if(httpException.getContent() != null){
            err.append(" - ").append(httpException.getContent());
        }
        if(httpException.getMessage() != null){
            err.append(" - ").append(httpException.getMessage());
        }
        final String message = err.toString().toLowerCase();

        if(message.contains("invalid credentials") || message.contains("authError")){
            if(refreshUserCredentialsById(userId) == null) {
                // Invalid Credentials and it is not possible to generate a new token, disconnect user
                logger.info(String.format("Invalid credentials for user [%s] - disconnecting", userId));
                disconnectUser(userId, null, functionId, true);
            }
        }
    }

    public Json checkUserById(final String userId){
        if(StringUtils.isNotBlank(userId)) {
            Json conf = getUserConfiguration(userId);
            if (conf != null && !conf.isEmpty()) {
                Json checkedToken = client.checkTokenFromConfiguration(userId, conf);
                if (StringUtils.isNotBlank(checkedToken.string(PROPERTY_ERROR))) {
                    logger.info(String.format("Invalid token for user [%s]: %s", userId, checkedToken.string(PROPERTY_ERROR)));
                } else {
                    conf.set(PROPERTY_TOKEN, checkedToken.string(PROPERTY_TOKEN));
                    conf.set(PROPERTY_REFRESH_TOKEN, checkedToken.string(PROPERTY_REFRESH_TOKEN));
                    conf.set(PROPERTY_EXPIRATION_TIME, checkedToken.string(PROPERTY_EXPIRATION_TIME));

                    return saveUserConfiguration(userId, conf, false);
                }
            } else {
                logger.info(String.format("User [%s] is not connected", userId));
            }
        } else {
            logger.info(String.format("Invalid user id [%s]", userId));
        }
        return null;
    }

    public Json checkUserOrDisconnect(final String userId, final String functionId){
        try {
            return checkUserById(userId);
        } catch (Exception ex){
            if(ex.toString().contains("\\\"invalid_grant\\\"") || ex.toString().contains("Token has been revoked.") ||
                    ex.toString().contains("Token has been expired or revoked.") || ex.toString().contains("Error renewing the token [401 Unauthorized]")){
                // token was revoked on Google service
                logger.info(String.format("Token for user [%s] has been revoked. Disconnecting user.", userId));
                disconnectUser(userId, null, functionId, false);
            }
            throw ex;
        }
    }

    public Json refreshUserCredentialsById(final String userId){
        if(StringUtils.isNotBlank(userId)) {
            Json conf = getUserConfiguration(userId);
            if (conf != null && !conf.isEmpty()) {
                Json checkedToken = client.checkToken(userId, null, conf.string(ValidToken.REFRESH_TOKEN), null);
                if (StringUtils.isBlank(checkedToken.string(PROPERTY_ERROR))) {
                    conf.set(PROPERTY_TOKEN, checkedToken.string(PROPERTY_TOKEN));
                    conf.set(PROPERTY_REFRESH_TOKEN, checkedToken.string(PROPERTY_REFRESH_TOKEN));
                    conf.set(PROPERTY_EXPIRATION_TIME, checkedToken.string(PROPERTY_EXPIRATION_TIME));

                    return saveUserConfiguration(userId, conf, false);
                } else {
                    logger.info(String.format("Invalid token for user [%s]: %s", userId, checkedToken.string(PROPERTY_ERROR)));
                }
            } else {
                logger.info(String.format("User [%s] is not connected", userId));
            }
        } else {
            logger.info(String.format("Invalid user id [%s]", userId));
        }
        return null;
    }

    private GoogleCalendarService getService(Json body, String userId, String userEmail, String functionId){
        String token = null;
        Json checkedConf = null;
        if(StringUtils.isNotBlank(userId)){
            checkedConf = checkUserOrDisconnect(userId, functionId);
        } else if(body != null && StringUtils.isNotBlank(body.string(PROPERTY_TOKEN))){
            token = body.string(PROPERTY_TOKEN);
        }
        if(StringUtils.isBlank(token) && StringUtils.isNotBlank(userId)){
            final Json conf = checkedConf != null ? checkedConf : getUserConfiguration(userId);
            if(conf != null && StringUtils.isNotBlank(conf.string(PROPERTY_TOKEN))){
                token = conf.string(PROPERTY_TOKEN);
            }
        }
        if(StringUtils.isNotBlank(token)){
            return client.getService(userId, token, this);
        } else {
            if(StringUtils.isNotBlank(userId)) {
                logger.info(String.format("Token was not generated for user [%s]", userId));
                disconnectUser(userId, userEmail, functionId, false);
            }
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Invalid user configuration");
        }
    }

    private OldGoogleCalendarService getOldService(Json body, String userId, String userEmail, String functionId){
        String token = null;
        Json checkedConf = null;
        if(StringUtils.isNotBlank(userId)){
            checkedConf = checkUserOrDisconnect(userId, functionId);
        } else if(body != null && StringUtils.isNotBlank(body.string(PROPERTY_TOKEN))){
            token = body.string(PROPERTY_TOKEN);
        }
        if(StringUtils.isBlank(token) && StringUtils.isNotBlank(userId)){
            final Json conf = checkedConf != null ? checkedConf : getUserConfiguration(userId);
            if(conf != null && StringUtils.isNotBlank(conf.string(PROPERTY_TOKEN))){
                token = conf.string(PROPERTY_TOKEN);
            }
        }
        if(StringUtils.isNotBlank(token)){
            return client.getOldService(userId, token, this);
        } else {
            if(StringUtils.isNotBlank(userId)) {
                logger.info(String.format("Token was not generated for user [%s]", userId));
                disconnectUser(userId, userEmail, functionId, false);
            }
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Invalid user configuration");
        }
    }

    @EndpointFunction(name = "authenticationUrl")
    public Json getAuthenticationUrl(FunctionRequest request){
        return Json.map().setIfNotNull("url", client.generateAuthURL());
    }

    @EndpointWebService(path = "/")
    @EndpointWebService(path = "callback")
    public String callback(){
        return "ok";
    }

    @EndpointFunction(name = "getUserInformation")
    public Json getUserInformation(FunctionRequest request){
        final String userId = request.getUserId();
        appLogs.info(String.format("Request to GET USER INFORMATION received [%s]", userId));

        boolean connected = false;
        Json information = null;

        if(StringUtils.isNotBlank(userId)) {
            final Json configuration = checkUserById(userId);
            final String token = configuration.string(PROPERTY_TOKEN);

            if(StringUtils.isNotBlank(token)) {
                final GoogleAuthenticationService authenticationService = client.getAuthenticationService(token);

                information = authenticationService.getUserInformation();
                connected = true;
            }
        }

        final Json response = Json.map().set("status", connected).setIfNotEmpty("information", information);
        logger.info(String.format("Function GET USER INFORMATION: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_findCalendars")
    public Json findCalendars(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to FIND CALENDARS received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.findCalendars(data, functionId);
        logger.info(String.format("Function FIND CALENDARS: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_findOneCalendar")
    public Json findOneCalendar(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to FIND ONE CALENDAR received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.findOneCalendar(data.string(PARAMETER_CALENDAR_ID), data, functionId);
        if(response == null){
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Resource not found");
        }
        logger.info(String.format("Function FIND ONE CALENDAR: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_createCalendar")
    public Json oldFunctionCreateCalendar(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to CREATE CALENDAR received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.createCalendar(data, functionId);
        logger.info(String.format("Function CREATE CALENDAR: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_updateCalendar")
    public Json updateCalendar(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to UPDATE CALENDAR received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.updateCalendar(data, functionId);
        logger.info(String.format("Function UPDATE CALENDAR [%s]: [%s]", data.string(PARAMETER_CALENDAR_ID), response.toString()));
        return response;
    }

    @EndpointFunction(name = "_deleteCalendar")
    public Json deleteCalendar(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to DELETE CALENDAR received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.deleteCalendar(data.string(PARAMETER_CALENDAR_ID), data, functionId);
        logger.info(String.format("Function DELETE CALENDAR [%s]: [%s]", data.string(PARAMETER_CALENDAR_ID), response.toString()));
        return response;
    }

    @EndpointFunction(name = "_findEvents")
    public Json findEvents(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to FIND EVENTS received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.findEvents(data.string(PARAMETER_CALENDAR_ID), data, functionId);
        logger.info(String.format("Function FIND EVENTS: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_findOneEvent")
    public Json findOneEvent(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to FIND ONE EVENT received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.findOneEvent(data.string(PARAMETER_CALENDAR_ID), data.string(PARAMETER_EVENT_ID), data, functionId);
        if(response == null){
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Resource not found");
        }
        logger.info(String.format("Function FIND ONE EVENT: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_createEvent")
    public Json createEvent(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to CREATE EVENT received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.createEvent(data.string(PARAMETER_CALENDAR_ID), data, functionId);
        logger.info(String.format("Function CREATE EVENT [%s]: [%s]", data.string(PARAMETER_CALENDAR_ID), response.toString()));
        return response;
    }

    @EndpointFunction(name = "_updateEvent")
    public Json updateEvent(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to UPDATE EVENT received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.updateEvent(data.string(PARAMETER_CALENDAR_ID), data.string(PARAMETER_EVENT_ID), data, functionId);
        logger.info(String.format("Function UPDATE EVENT [%s][%s]: [%s]", data.string(PARAMETER_CALENDAR_ID), data.string(PARAMETER_EVENT_ID), response.toString()));
        return response;
    }

    @EndpointFunction(name = "_deleteEvent")
    public Json deleteEvent(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("Request to DELETE EVENT received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.deleteEvent(data.string(PARAMETER_CALENDAR_ID), data.string(PARAMETER_EVENT_ID), data, functionId);
        logger.info(String.format("Function DELETE EVENT [%s][%s]: [%s]", data.string(PARAMETER_CALENDAR_ID), data.string(PARAMETER_EVENT_ID), response.toString()));
        return response;
    }

    @EndpointFunction(name = "_getRequest")
    public Json getRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("GET request received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.getRequest(data.string("path"), functionId);
        logger.info(String.format("Function GET: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_postRequest")
    public Json postRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("POST request received", data);

        final Json content = getContent(data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.postRequest(data.string("path"), content, functionId);
        logger.info(String.format("Function POST: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_putRequest")
    public Json putRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("PUT request received", data);

        final Json content = getContent(data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.putRequest(data.string("path"), content, functionId);
        logger.info(String.format("Function PUT: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_patchRequest")
    public Json patchRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("PATCH request received", data);

        final Json content = getContent(data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.patchRequest(data.string("path"), content, functionId);
        logger.info(String.format("Function PATCH: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_deleteRequest")
    public Json deleteRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("DELETE request received", data);

        final GoogleCalendarService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.deleteRequest(data.string("path"), functionId);
        logger.info(String.format("Function DELETE: [%s]", response.toString()));
        return response;
    }

    private Json getContent(Json body) {
        Json content = body.json("body");
        if(content == null) {
            content = body.json("params");
        }
        if(content == null) {
            content = Json.map();
        }
        return content;
    }

    @EndpointFunction(name = "_oldFunction")
    public Json oldFunction(FunctionRequest request){
        final Json data = request.getJsonParams();
        if(data == null){
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Invalid function request");
        }

        final String function = data.string(OLD_FUNCTION_NAME);
        if(StringUtils.isBlank(function)){
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Invalid function request");
        }
        data.remove(OLD_FUNCTION_NAME);

        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();

        final OldGoogleCalendarService service = getOldService(data, userId, request.getUserEmail(), functionId);

        Json response = null;
        switch (function){
            case OLD_METHOD_GET_CALENDARS:
                appLogs.info("OLD GET CALENDARS request received", data);
                response = oldFunctionGetCalendars(service, functionId);
                logger.info(String.format("Function OLD GET CALENDARS: [%s]", response.toString()));
                break;
            case OLD_METHOD_CREATE_CALENDAR:
                appLogs.info("OLD CREATE CALENDAR request received", data);
                response = oldFunctionCreateCalendar(service, data, functionId);
                logger.info(String.format("Function OLD CREATE CALENDAR: [%s]", response.toString()));
                break;
            case OLD_METHOD_UPDATE_CALENDAR:
                appLogs.info("OLD UPDATE CALENDAR request received", data);
                response = oldFunctionUpdateCalendar(service, data, functionId);
                logger.info(String.format("Function OLD UPDATE CALENDAR: [%s]", response.toString()));
                break;
            case OLD_METHOD_REMOVE_CALENDAR:
                appLogs.info("OLD REMOVE CALENDAR request received", data);
                response = oldFunctionRemoveCalendar(service, data, functionId);
                logger.info(String.format("Function OLD REMOVE CALENDAR: [%s]", response.toString()));
                break;
            case OLD_METHOD_CLEAR_CALENDAR:
                appLogs.info("OLD CLEAR CALENDAR request received", data);
                response = oldFunctionClearCalendar(service, data, functionId);
                logger.info(String.format("Function OLD CLEAR CALENDAR: [%s]", response.toString()));
                break;
            case OLD_METHOD_GET_EVENTS:
                appLogs.info("OLD GET EVENTS request received", data);
                response = oldFunctionGetEvents(service, data, functionId);
                logger.info(String.format("Function OLD GET EVENTS: [%s]", response.toString()));
                break;
            case OLD_METHOD_SYNC_EVENTS:
                appLogs.info("OLD SYNC EVENT request received", data);
                response = oldFunctionSyncEvents(service, data, functionId);
                logger.info(String.format("Function OLD SYNC EVENT: [%s]", response.toString()));
                break;
            case OLD_METHOD_CREATE_EVENT:
                appLogs.info("OLD CREATE EVENT request received", data);
                response = oldFunctionCreateEvent(service, data, functionId);
                logger.info(String.format("Function OLD CREATE EVENT: [%s]", response.toString()));
                break;
            case OLD_METHOD_UPDATE_EVENT:
                appLogs.info("OLD UPDATE EVENT request received", data);
                response = oldFunctionUpdateEvent(service, data, functionId);
                logger.info(String.format("Function OLD UPDATE EVENT: [%s]", response.toString()));
                break;
            case OLD_METHOD_REMOVE_EVENT:
                appLogs.info("OLD REMOVE EVENT request received", data);
                response = oldFunctionRemoveEvent(service, data, functionId);
                logger.info(String.format("Function OLD REMOVE EVENT: [%s]", response.toString()));
                break;
        }
        return response == null ? Json.map() : response;
    }

    private Json oldFunctionGetCalendars(final OldGoogleCalendarService service, final String functionId){
        return service.calendarList(functionId);
    }

    private Json oldFunctionCreateCalendar(final OldGoogleCalendarService service, final Json body, final String functionId){
        return service.createCalendar(
                        body.string("summary"),
                        body.string("description"),
                        body.string("location"),
                        body.string("timezone"),
                        body.json("data"),
                        functionId
                );
    }

    private Json oldFunctionUpdateCalendar(final OldGoogleCalendarService service, final Json body, final String functionId){
        return service.patchCalendar(
                        body.string("calendarId"),
                        body.string("summary"),
                        body.string("description"),
                        body.string("location"),
                        body.string("timezone"),
                        body.json("data"),
                        functionId
                );
    }

    private Json oldFunctionRemoveCalendar(final OldGoogleCalendarService service, final Json body, final String functionId){
        return service.removeCalendar(
                body.string("calendarId"),
                functionId
        );
    }

    private Json oldFunctionClearCalendar(final OldGoogleCalendarService service, final Json body, final String functionId){
        return service.clearCalendar(
                        body.string("calendarId"),
                        functionId
                );
    }

    private Json oldFunctionGetEvents(final OldGoogleCalendarService service, final Json body, final String functionId){
        return service.eventsList(
                        body.string("calendarId"),
                        body.string("query"),
                        body.object("from"),
                        body.object("to"),
                        body.string("timezone"),
                        body.string("pageToken"),
                        body.integer("maxResults"),
                        body.string("queryToken"),
                        body.json("data"),
                        functionId
                );
    }

    private Json oldFunctionSyncEvents(final OldGoogleCalendarService service, final Json body, final String functionId){
        return service.eventsSync(body.string("calendarId"), body.string("queryToken"), functionId);
    }

    private Json oldFunctionCreateEvent(final OldGoogleCalendarService service, final Json body, final String functionId){
        return service.createEvent(
                        body.string("calendarId"),
                        body.string("start"),
                        body.object("end"),
                        body.bool("allDayEvent"),
                        body.string("timezone"),
                        body.string("summary"),
                        body.string("description"),
                        body.string("location"),
                        body.json("data"),
                        functionId
                );
    }

    private Json oldFunctionUpdateEvent(final OldGoogleCalendarService service, final Json body, final String functionId){
        return service.patchEvent(
                        body.string("eventId"),
                        body.string("calendarId"),
                        body.string("start"),
                        body.object("end"),
                        body.bool("allDayEvent"),
                        body.string("timezone"),
                        body.string("summary"),
                        body.string("description"),
                        body.string("location"),
                        body.json("data"),
                        functionId
                );
    }

    private Json oldFunctionRemoveEvent(final OldGoogleCalendarService service, final Json body, final String functionId){
        return service.removeEvent(
                        body.string("calendarId"),
                        body.string("eventId"),
                        functionId
                );
    }

    private void pollingProcess(){
        final long timerCounter = pollingCounter.getAndIncrement();

        logger.info(logSync(timerCounter, "------------------------------ INI"));
        logger.info(logSync(timerCounter, "- Sync process started ..."));
        try {
            long processedEvents = 0;
            try {
                final DataStoreResponse usersResponse = usersDataStore.find();
                if(usersResponse != null && usersResponse.getItems() != null && !usersResponse.getItems().isEmpty()){
                    final List<Json> users = usersResponse.getItems();
                    logger.info(logSync(timerCounter, String.format("%s=%s - Sync users", TAG_USERS, users.size())));

                    for (int userCounter = 0; userCounter < users.size(); userCounter++) {
                        final Json user = users.get(userCounter);
                        if (user == null || user.isEmpty() || !user.contains("_id") || StringUtils.isBlank(user.string("_id"))) {
                            logger.warn(logSync(timerCounter, userCounter, String.format("- Invalid user [%s]", user)));
                        } else {
                            final String userId = user.string("_id");
                            logger.info(logSync(timerCounter, userCounter, String.format("%s=%s - Sync user", TAG_USER_ID, userId)));
                            try {
                                final List<Json> items = syncUser(userId, timerCounter, userCounter);
                                for (int itemCounter = 0; itemCounter < items.size(); itemCounter++) {
                                    try {
                                        final Json item = items.get(itemCounter);
                                        boolean processed = false;
                                        if(item != null && !item.isEmpty()){
                                            final String eventName = getEventName(item);
                                            if(StringUtils.isNotBlank(eventName)) {
                                                logger.debug(logSync(timerCounter, userCounter, itemCounter, String.format("%s=%s - %s [%s]", TAG_EVENT, eventName, ITEM_NAME, item)));
                                                events().send(eventName, item, null, userId);
                                                processed = true;
                                            }
                                        }
                                        if(processed) {
                                            // send event
                                            processedEvents++;
                                        } else {
                                            logger.info(logSync(timerCounter, userCounter, itemCounter, String.format("- Invalid %s [%s]", ITEM_NAME, item)));
                                        }
                                    } catch (Exception exe){
                                        logger.info(logSync(timerCounter, userCounter, itemCounter, String.format("- Error when try to send %s [%s]", ITEM_NAME, exe.getMessage())));
                                    }
                                }
                            } catch (Exception exu) {
                                logger.info(logSync(timerCounter, userCounter, String.format("- Error when try to process user %s [%s]", ITEMS_NAME, exu.getMessage())));
                            }
                        }
                    }
                } else {
                    logger.info(logSync(timerCounter, String.format("%s=%s - There is not users to sync", TAG_USERS, 0)));
                }
            } catch (Exception ex) {
                logger.info(logSync(timerCounter, String.format("- Error when try to execute sync [%s]", ex.getMessage())));
            }
            logger.info(logSync(timerCounter, String.format("%s=%s - Events sent", TAG_EVENTS_TOTAL, processedEvents)));
        } catch (Exception ex){
            logger.error(logSync(timerCounter, String.format("Error when executes sync process: %s", ex.getMessage())), ex);
        }
        logger.info(logSync(timerCounter, "- Finished sync process"));
        logger.info(logSync(timerCounter, "------------------------------ END"));
    }

    private List<Json> syncUser(String userId, long timerCounter, int userCounter) {
        // list of events to send to application
        final List<Json> eventsResponse = new ArrayList<>();

        this.userSyncLock.lock();
        try {
            // restore information about the last sync process over the user
            Json lastSync;
            try {
                lastSync = pollingDataStore.findById(userId);
            } catch (Exception ex){
                logger.info(logSync(timerCounter, userCounter, String.format("First sync for user [%s]", userId)));
                lastSync = null;
            }
            Json lastCSync = lastSync != null ? lastSync.json(PARAMETER_CALENDARS) : null;
            if(lastCSync == null){
                lastCSync = Json.map();
            }
            final Json lastCalSync = lastCSync;

            final GoogleCalendarService service = getService(null, userId, null, null);

            //  retrieve the current calendar list of the user from the Google service
            final Json calendars = service.findAllCalendars();
            if(calendars == null || calendars.isEmpty()){
                logger.info(logSync(timerCounter, userCounter, String.format("%s=%s - No calendars found - Empty response", TAG_LAST_SYNC, lastCalSync.size())));
            } else if (calendars.is(Parameter.EXCEPTION_FLAG)) {
                logger.warn(logSync(timerCounter, userCounter, String.format("- Error when try to synchronize events. Exception [%s]", calendars.toString())));
                final String message = calendars.string("message");
                if (StringUtils.isNotBlank(message)) {
                    appLogs.error(String.format("Google API exception: %s", message));
                } else {
                    appLogs.error(String.format("Google API exception: %s", calendars.toString()));
                }
            } else if (!calendars.contains("calendars")) {
                logger.info(logSync(timerCounter, userCounter, String.format("%s=%s - No calendars found", TAG_LAST_SYNC, lastCalSync.size())));
            } else {
                // information about the current sync process
                final Json newCalSync = Json.map();

                final List<Json> calendarList = calendars.jsons("calendars");
                logger.info(logSync(timerCounter, userCounter, String.format("%s=%s %s=%s - Calendars found", TAG_CALENDARS, calendarList.size(), TAG_LAST_SYNC, lastCalSync.size())));

                // for each current calendar
                calendarList.forEach(calendar -> {
                    final String calendarId = calendar.string("id");
                    if (StringUtils.isNotBlank(calendarId)) {
                        try {
                            final String calendarKey = MapsUtils.cleanDotKey(calendarId);
                            final String lastQueryToken = lastCalSync.string(calendarKey);

                            // keep last query token
                            newCalSync.set(calendarKey, lastQueryToken);

                            // get the events with the last query token (or null if is the first sync process over the calendar)
                            final Json response = service.eventsSync(calendarId, lastQueryToken, null);

                            if (response.is(Parameter.EXCEPTION_FLAG)) {
                                logger.warn(logSync(timerCounter, userCounter, String.format("%s=%s - Error when try to synchronize events of calendar. Exception [%s]",
                                        TAG_CALENDAR, calendarKey,
                                        response.toString())));
                                final String message = response.string("message");
                                if (StringUtils.isNotBlank(message)) {
                                    appLogs.error(String.format("Google API exception: %s", message));
                                } else {
                                    appLogs.error(String.format("Google API exception: %s", response.toString()));
                                }
                            } else {
                                final String newQueryToken = response.string("queryToken");
                                if (StringUtils.isNotBlank(newQueryToken)) {
                                    // save new query token
                                    newCalSync.set(calendarKey, newQueryToken);

                                    // check if these events are new ones
                                    boolean sameTokens = true;
                                    if (!newQueryToken.equals(lastQueryToken)) {
                                        sameTokens = false;

                                        // add events from the calendar to the events list
                                        final List<Json> newEvents = response.jsons("events");

                                        if (newEvents != null && !newEvents.isEmpty()) {
                                            eventsResponse.addAll(newEvents);
                                            logger.info(logSync(timerCounter, userCounter, String.format("%s=%s %s=%s - Calendar events",
                                                    TAG_CALENDAR, calendarKey,
                                                    TAG_EVENTS, newEvents.size())));
                                        } else {
                                            logger.info(logSync(timerCounter, userCounter, String.format("%s=%s %s=0 - No calendar events",
                                                    TAG_CALENDAR, calendarKey,
                                                    TAG_EVENTS)));
                                        }
                                    }
                                    logger.info(logSync(timerCounter, userCounter, String.format("%s=%s %s=%s %s=%s - Tokens",
                                            TAG_CALENDAR, calendarKey,
                                            TAG_TOKEN_LAST, lastQueryToken,
                                            TAG_TOKEN_NEW, sameTokens ? "no_change" : newQueryToken)));
                                } else {
                                    logger.info(logSync(timerCounter, userCounter, String.format("%s=%s %s=%s %s=empty - Tokens",
                                            TAG_CALENDAR, calendarKey,
                                            TAG_TOKEN_LAST, lastQueryToken,
                                            TAG_TOKEN_NEW)));
                                }
                            }
                        } catch (Exception ex) {
                            logger.warn(logSync(timerCounter, userCounter, String.format("%s=%s - Error when try to synchronize calendar. Exception [%s]",
                                    TAG_CALENDAR_ID, calendarId,
                                    ex.toString())), ex);
                        }
                    }
                });

                // save the new sync information on the data store
                pollingDataStore.save(Json.map()
                        .set(PARAMETER_ID, userId)
                        .set(PARAMETER_CALENDARS, newCalSync)
                        .set(PARAMETER_LAST_SYNC, System.currentTimeMillis())
                        .set(Parameter.DATA_STORE_TTL, MAX_WAITING_BETWEEN_SYNCS)
                );
            }

        } catch (Exception ex) {
            logger.warn(logSync(timerCounter, userCounter, String.format("- Error when try to synchronize calendars. Exception [%s]", ex.toString())));
        } finally {
            this.userSyncLock.unlock();
        }
        logger.info(logSync(timerCounter, userCounter, String.format("%s=%s - Events sent", TAG_EVENTS, eventsResponse.size())));

        // return the list of events to send to application
        return eventsResponse;
    }

    private static String logSync(long timerCounter, String log){
        return String.format("%s=%s %s", TAG_SYNC, timerCounter, log);
    }

    private static String logSync(long timerCounter, int userCounter, String log){
        return String.format("%s=%s %s=%s %s", TAG_SYNC, timerCounter, TAG_USER, userCounter, log);
    }

    private static String logSync(long timerCounter, int userCounter, int itemCounter, String log){
        return String.format("%s=%s %s=%s %s=%s %s", TAG_SYNC, timerCounter, TAG_USER, userCounter, TAG_EVENT_NUM, itemCounter, log);
    }

    private String getEventName(Json item){
        String eventName = null;
        if(item != null) {
            if (PARAMETER_STATUS_CANCELLED.equalsIgnoreCase(item.string(PARAMETER_STATUS))){
                eventName = SYNC_EVENT_DELETED;
            } else {
                eventName = SYNC_EVENT_UPDATED;
            }
        }
        return eventName;
    }
}
