package io.slingr.endpoints.googlecalendar;

import io.slingr.endpoints.services.exchange.ReservedName;
import io.slingr.endpoints.services.rest.RestMethod;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.tests.EndpointTests;
import io.slingr.endpoints.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * <p>Test over the GoogleCalendarEndpoint class
 *
 * <p>Created by lefunes on 08/11/15.
 */
public class GoogleCalendarEndpointTest {

    private static final Logger logger = Logger.getLogger(GoogleCalendarEndpointTest.class);

    // random user id
    private static final String USER_ID = "Adpl3eDSsZ1kcuOkdABcCRHDF";

    // ----------------------------------------------------------------
    // STEPS TO GENERATE TOKENS FOR TESTS
    // ----------------------------------------------------------------
    // 1º) Generate code using the browser using the URL generated by 'testAuthenticationUrl()' test case
    // Change the USER_CODE with the new code:
    private static final String USER_CODE = "4/AAB815SICWFOCvqtXsGJmrdzpalua_3i_sg38-lnpQ8GRZIcfUtCpM45MZkUX0ophDPAzSbqBuzcks_SzqeEcGg#";
    // ----------------------------------------------------------------
    // 2º) Generate the refresh token using the 'testGenerateAndSaveTokens()' test case
    // Change the credentials with the information generated:
    private static final String REFRESH_TOKEN = "1/1r3f9RP212lwlf-V4TDu2GUWVeIFyIEy01fMC2jo8CPjLsPRMoQHmTpT5y4v2NPL";
    private static final String USER_NAME = "Test Integrations";
    private static final String USER_PICTURE = "https://lh6.googleusercontent.com/-TqRx4AMDL7g/AAAAAAAAAAI/AAAAAAAAABY/IYsn51oD9cc/photo.jpg";
    // ----------------------------------------------------------------
    // 3º) You can invalidate a refresh token using the 'testInvalidateRefreshTokens()' test case
    // ----------------------------------------------------------------

    private static EndpointTests test;

    @BeforeClass
    public static void init() throws Exception {
        test = EndpointTests.start(new io.slingr.endpoints.googlecalendar.Runner(), "test.properties");
    }

    @Test
    public void testAuthenticationUrl(){
        logger.info("-- testing 'authenticationUrl' function");
        Json response;

        assertTrue(test.getReceivedEvents().isEmpty());

        response = test.executeFunction("authenticationUrl");
        assertNotNull(response);
        logger.info("------------------- AUTHENTICATION URL -----------------------");
        logger.info(String.format("URL [%s]", response.string("url")));
        assertEquals("https://accounts.google.com/o/oauth2/auth?access_type=offline&approval_prompt=force&client_id=107073249789-76q4jsoc9nvtmo2brr0um5ooi1co8eg4.apps.googleusercontent.com&redirect_uri=http://localhost:8000/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/calendar%20https://www.googleapis.com/auth/calendar.readonly&state=test1", response.string("url"));
        logger.info("--------------------------------------------------------------");

        assertTrue(test.getReceivedEvents().isEmpty());

        logger.info("-- END");
    }

    @Test
    @Ignore("Use to generate the tokens for the user to execute the tests")
    public void testGenerateAndSaveTokens(){
        logger.info("-- testing 'connect' function");
        Json response, configuration;

        cleanConnectedUsers();

        test.clearReceivedEvents();

        response = test.executeFunction(ReservedName.CONNECT_USER, Json.map().set(GoogleCalendarEndpoint.PROPERTY_CODE, USER_CODE), USER_ID);
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertEquals(USER_ID, response.string("userId"));

        configuration = response.json("configuration");
        assertNotNull(configuration);
        assertFalse(configuration.isEmpty());
        assertTrue(StringUtils.isNotBlank(configuration.string("refreshToken")));

        logger.info("--------------------- USER CONNECTED -------------------------");
        logger.info(String.format("User connected [%s]", configuration.string("result")));
        logger.info(String.format("Refresh Token [%s]", configuration.string("refreshToken")));
        logger.info(String.format("Name [%s]", configuration.string("name")));
        logger.info(String.format("Picture [%s]", configuration.string("picture")));
        logger.info("--------------------------------------------------------------");

        assertTrue(StringUtils.isNotBlank(configuration.string("token")));
        assertTrue(StringUtils.isNotBlank(configuration.string("expirationTime")));
        assertTrue(configuration.string("result").startsWith("Connection established as"));

        checkConnectedUser(false);

        final Json user = test.getUserDataStoreItems().get(0);
        assertEquals(user.string("_id"), configuration.string("_id"));
        assertEquals(user.string("expirationTime"), configuration.string("expirationTime"));
        assertEquals(user.string("lastCode"), configuration.string("lastCode"));
        assertEquals(user.string("name"), configuration.string("name"));
        assertEquals(user.string("picture"), configuration.string("picture"));
        assertEquals(user.string("refreshToken"), configuration.string("refreshToken"));
        assertEquals(user.string("result"), configuration.string("result"));
        assertEquals(user.string("token"), configuration.string("token"));

        List<Json> events = test.getReceivedEvents();
        assertFalse(events.isEmpty());
        assertEquals(1, events.size());

        Json event = events.get(0);
        assertNotNull(event);
        assertFalse(event.isEmpty());
        assertEquals(ReservedName.USER_CONNECTED, event.string("event"));
        assertEquals(USER_ID, event.string("userId"));

        Json data = event.json("data");
        assertNotNull(data);
        assertFalse(data.isEmpty());
        assertEquals(USER_ID, data.string("userId"));

        configuration = data.json("configuration");
        assertNotNull(configuration);
        assertFalse(configuration.isEmpty());
        assertEquals(user.string("_id"), configuration.string("_id"));
        assertEquals(user.string("expirationTime"), configuration.string("expirationTime"));
        assertEquals(user.string("lastCode"), configuration.string("lastCode"));
        assertEquals(user.string("name"), configuration.string("name"));
        assertEquals(user.string("picture"), configuration.string("picture"));
        assertEquals(user.string("refreshToken"), configuration.string("refreshToken"));
        assertEquals(user.string("result"), configuration.string("result"));
        assertEquals(user.string("token"), configuration.string("token"));

        test.clearReceivedEvents();

        logger.info("-- END");
    }

    @Test
    @Ignore("Use to disable a refresh token")
    public void testInvalidateRefreshTokens(){
        logger.info("-- testing 'disconnect' function");
        Json response, configuration, data;
        List<Json> contacts, events;

        test.clearReceivedEvents();

        response = test.executeFunction(ReservedName.DISCONNECT_USER, Json.map(), USER_ID);
        assertNotNull(response);
        assertFalse(response.isEmpty());

        configuration = response.json("configuration");
        assertNotNull(configuration);
        assertFalse(configuration.isEmpty());
        assertEquals("Connection disabled.", configuration.string("result"));

        events = test.getReceivedEvents();
        assertFalse(events.isEmpty());
        for (Json event : events) {
            assertNotNull(event);
            assertFalse(event.isEmpty());
            assertEquals(ReservedName.USER_DISCONNECTED, event.string("event"));
            assertEquals(USER_ID, event.string("userId"));
        }
        test.clearReceivedEvents();
        assertTrue(test.getReceivedEvents().isEmpty());

        response = test.executeFunction("_findCalendars", Json.map(), USER_ID, true);
        assertNotNull(response);
        assertEquals("Invalid user configuration", response.string("message"));

        events = test.getReceivedEvents();
        assertFalse(events.isEmpty());
        for (Json event : events) {
            assertNotNull(event);
            assertFalse(event.isEmpty());
            assertEquals(ReservedName.USER_DISCONNECTED, event.string("event"));
            assertEquals(USER_ID, event.string("userId"));
        }
        test.clearReceivedEvents();
        assertTrue(test.getReceivedEvents().isEmpty());

        // creates the default user on DB when the test begin
        createConnectedUser();

        response = test.executeFunction("_findCalendars", Json.map(), USER_ID);
        assertNotNull(response);
        contacts = response.jsons("contacts");
        assertNotNull(contacts);
        assertFalse(contacts.isEmpty());

        response = test.executeFunction(ReservedName.DISCONNECT_USER, Json.map(), USER_ID);
        assertNotNull(response);
        assertFalse(response.isEmpty());

        configuration = response.json("configuration");
        assertNotNull(configuration);
        assertFalse(configuration.isEmpty());
        assertEquals("Connection disabled.", configuration.string("result"));

        assertTrue(test.getUserDataStoreItems().isEmpty());

        // disconnect user, invalidate refresh token
        events = test.getReceivedEvents();
        assertFalse(events.isEmpty());
        for (Json event : events) {
            assertNotNull(event);
            assertFalse(event.isEmpty());
            assertEquals(ReservedName.USER_DISCONNECTED, event.string("event"));
            assertEquals(USER_ID, event.string("userId"));
        }
        test.clearReceivedEvents();
        assertTrue(test.getReceivedEvents().isEmpty());

        response = test.executeFunction("_findCalendars", Json.map(), USER_ID, true);
        assertNotNull(response);
        assertEquals("Invalid user configuration", response.string("message"));

        events = test.getReceivedEvents();
        assertFalse(events.isEmpty());
        for (Json event : events) {
            assertNotNull(event);
            assertFalse(event.isEmpty());
            assertEquals(ReservedName.USER_DISCONNECTED, event.string("event"));
            assertEquals(USER_ID, event.string("userId"));
        }
        test.clearReceivedEvents();
        assertTrue(test.getReceivedEvents().isEmpty());

        // waits for a while
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            // do nothing
        }

        // creates the default user on DB with the invalid refresh token
        createConnectedUser();

        response = test.executeFunction("_findCalendars", Json.map(), USER_ID, true);
        assertNotNull(response);
        assertTrue(response.string("message").contains("Token has been expired or revoked."));

        events = test.getReceivedEvents();
        assertFalse(events.isEmpty());
        for (Json event : events) {
            assertNotNull(event);
            assertFalse(event.isEmpty());
            assertEquals(ReservedName.USER_DISCONNECTED, event.string("event"));
            assertEquals(USER_ID, event.string("userId"));
        }
        test.clearReceivedEvents();
        assertTrue(test.getReceivedEvents().isEmpty());

        // cleans the users at the end of the test
        cleanConnectedUsers();

        assertTrue(test.getReceivedEvents().isEmpty());
        test.clearReceivedEvents();

        logger.info("-- END");
    }

    @Test
    public void functionConnectDisconnect(){
        logger.info("-- testing invalid 'connect' and 'disconnect' function");
        Json response;

        assertTrue(test.getReceivedEvents().isEmpty());

        response = test.executeFunction(ReservedName.DISCONNECT_USER, Json.map(), true);
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertEquals("User ID is required", response.string("message"));

        response = test.executeFunction(ReservedName.CONNECT_USER, Json.map(), true);
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertEquals("User ID is required", response.string("message"));

        assertTrue(test.getReceivedEvents().isEmpty());

        logger.info("-- END");
    }

    @Test
    public void wsCallback(){
        logger.info("-- testing '/callback' web services");
        WebServiceResponse response;

        assertTrue(test.getReceivedEvents().isEmpty());

        response = test.executeWebServices(RestMethod.GET, "/callback");
        assertNotNull(response);
        assertEquals("ok", response.getBody());

        response = test.executeWebServices(RestMethod.HEAD, "/callback");
        assertNotNull(response);
        assertEquals("ok", response.getBody());

        response = test.executeWebServices(RestMethod.POST, "/callback");
        assertNotNull(response);
        assertEquals("ok", response.getBody());

        response = test.executeWebServices(RestMethod.PUT, "/callback");
        assertNotNull(response);
        assertEquals("ok", response.getBody());

        assertTrue(test.getReceivedEvents().isEmpty());

        logger.info("-- END");
    }

    @Test
    public void testGetUserInformation() {
        logger.info("-- testing mocked user information");

        // creates the default user on DB when the test begin
        createConnectedUser();

        // cleans the users at the end of the test
        cleanConnectedUsers();

        logger.info("-- END");
    }

    /**
     * Creates a default user
     */
    private void createConnectedUser(){
        cleanConnectedUsers();

        test.addUserDataStoreItem(Json.map()
                .set("_id", USER_ID)
                .set("expirationTime", "2017-07-12T18:00:22.203-0300") // expired token
                .set("lastCode", USER_CODE)
                .set("name", USER_NAME)
                .set("picture", USER_PICTURE)
                .set("refreshToken", REFRESH_TOKEN)
                .set("result", String.format("Connection established as %s.", USER_NAME))
                .set("token", "-")
        );

        checkConnectedUser();
    }

    /**
     * Check the stored default user
     */
    private void checkConnectedUser(){
        checkConnectedUser(true);
    }

    /**
     * Check the stored default user
     */
    private void checkConnectedUser(boolean checkRefreshToken){
        assertFalse(test.getUserDataStoreItems().isEmpty());
        final Json user = test.getUserDataStoreItems().get(0);
        assertEquals(USER_ID, user.string("_id"));
        assertEquals(USER_CODE, user.string("lastCode"));
        assertEquals(USER_NAME, user.string("name"));
        assertEquals(USER_PICTURE, user.string("picture"));
        if(checkRefreshToken) {
            assertEquals(REFRESH_TOKEN, user.string("refreshToken"));
        }
        assertEquals(String.format("Connection established as %s.", USER_NAME), user.string("result"));
        assertTrue(StringUtils.isNotBlank(user.string("expirationTime")));
        assertFalse(StringUtils.isBlank(user.string("token")));
    }

    /**
     * Delete the default user
     */
    private void cleanConnectedUsers(){
        test.clearUserDataStore();
        assertTrue(test.getUserDataStoreItems().isEmpty());
    }

    @Test
    public void functionFindCalendars(){
        logger.info("-- testing FIND CALENDARS function");
        Json response;
        List<Json> items;

        // creates the default user on DB when the test begin
        createConnectedUser();

        response = test.executeFunction("_findCalendars", Json.map(), USER_ID);
        assertNotNull(response);
        items = response.jsons("items");
        assertNotNull(items);
        assertFalse(items.isEmpty());

        logger.info("------------------------------------");
        final int cSize = items.size();
        logger.info(String.format("CALENDARS [%s]", cSize));
        for (Json item : items) {
            logger.info(String.format(" - [%s] [%s]: %s", item.string("summary"), item.string("id"), item.toString()));
        }
        logger.info("------------------------------------");

        // cleans the users at the end of the test
        cleanConnectedUsers();
        assertTrue(test.getReceivedEvents().isEmpty());

        logger.info("-- END");
    }

    @Test
    public void functionGetUserInformation(){
        logger.info("-- testing GET USER INFORMATION function");
        Json response;

        // creates the default user on DB when the test begin
        createConnectedUser();

        response = test.executeFunction("getUserInformation", Json.map(), USER_ID);
        assertNotNull(response);
        assertTrue(response.bool("status"));

        Json information = response.json("information");
        assertNotNull(information);
        assertNotNull(information.string("id"));
        assertEquals(USER_NAME, information.string("name"));
        assertNotNull(information.string("given_name"));
        assertTrue(USER_NAME.contains(information.string("given_name")));
        assertNotNull(information.string("family_name"));
        assertTrue(USER_NAME.contains(information.string("family_name")));
        assertEquals("en", information.string("locale"));
        assertEquals(USER_PICTURE, information.string("picture"));

        // cleans the users at the end of the test
        cleanConnectedUsers();
        assertTrue(test.getReceivedEvents().isEmpty());

        logger.info("-- END");
    }

    @Test
    public void functionAllCalendars(){
        logger.info("-- testing ALL CALENDARS function");
        Json response;

        // creates the default user on DB when the test begin
        createConnectedUser();

        response = test.executeFunction("_oldFunction", Json.map().set("__functionName", "getCalendars"), USER_ID);
        assertNotNull(response);
        final List<Json> items1 = response.jsons("calendars");
        assertNotNull(items1);
        assertFalse(items1.isEmpty());

        response = test.executeFunction("_getRequest", Json.map().set("path", "users/me/calendarList"), USER_ID);
        assertNotNull(response);
        final List<Json> items2 = response.jsons("items");
        assertNotNull(items2);
        assertFalse(items2.isEmpty());

        response = test.executeFunction("_findCalendars", Json.map(), USER_ID);
        assertNotNull(response);
        final List<Json> items3 = response.jsons("items");
        assertNotNull(items3);
        assertFalse(items3.isEmpty());

        // all the list have the same number of items
        assertEquals(items1.size(), items2.size());
        assertEquals(items2.size(), items3.size());

        // cleans the users at the end of the test
        cleanConnectedUsers();
        assertTrue(test.getReceivedEvents().isEmpty());

        logger.info("-- END");
    }
}
