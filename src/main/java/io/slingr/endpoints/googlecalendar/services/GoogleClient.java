package io.slingr.endpoints.googlecalendar.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.exceptions.PermanentException;
import io.slingr.endpoints.googlecalendar.GoogleCalendarEndpoint;
import io.slingr.endpoints.googlecalendar.services.entities.ValidToken;
import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleClient {

    private static final Logger logger = LoggerFactory.getLogger(GoogleClient.class);

    private static final String ACCESS_TYPE = "offline";
    private static final String RESPONSE_TYPE = "code";
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	private final String application;
	private final String clientId;
    private final String clientSecret;
    private final String defaultRedirectUri;
    private final List<ServiceType> services;

	public GoogleClient(String application, String clientId, String clientSecret, String redirectUri, ServiceType... services) {
		this.application = application;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
        this.defaultRedirectUri = redirectUri;

        this.services = new ArrayList<>();
        if(services != null && services.length > 0) {
            for (ServiceType service : services) {
                if(!this.services.contains(service)){
                    this.services.add(service);
                }
            }
            if(!this.services.contains(ServiceType.OAUTH_2)){
                this.services.add(ServiceType.OAUTH_2);
            }
        }
	}

    public String generateAuthURL() {
        final List<String> scopes = new ArrayList<>();
        services.stream().forEach(service ->
                service.getScopes().stream()
                    .filter(scope -> !scopes.contains(scope))
                    .forEach(scopes::add)
        );

		return new GoogleAuthorizationCodeRequestUrl(clientId, defaultRedirectUri, scopes)
                .setAccessType(ACCESS_TYPE)
                .setApprovalPrompt("force")
                .setResponseTypes(Collections.singletonList(RESPONSE_TYPE))
                .setState(application)
                .build();
    }

    public Json generateTokensFromCode(String code, String redirectUri) throws EndpointException {
        String error = null;
        ValidToken validToken = null;
        boolean permanentException = true;

        if (code == null) {
            error = "Invalid user code: null";
        } else {
            try {
                final GoogleAuthorizationCodeTokenRequest request = new GoogleAuthorizationCodeTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), new JacksonFactory(), clientId, clientSecret, code, StringUtils.isNotBlank(redirectUri) ? redirectUri : defaultRedirectUri);
                validToken = new ValidToken(request.execute());
            } catch (HttpResponseException e) {
                error = String.format("Invalid response when try to generate code [%s]", e.getContent() != null ? e.getContent() : e.getMessage());
                permanentException = false;
            } catch (Exception e) {
                error = String.format("Error getting the token [%s]", e.getMessage());
            }
        }
        if(StringUtils.isBlank(error)){
            if(validToken != null) {
                return validToken.toJson();
            } else {
                throw EndpointException.permanent(ErrorCode.API, "Invalid token: null");
            }
        } else {
            if(permanentException) {
                throw EndpointException.permanent(ErrorCode.API, error);
            } else {
                throw EndpointException.retryable(ErrorCode.API, error);
            }
        }
    }

    public Json checkTokenFromConfiguration(String userId, Json configuration) throws EndpointException {
        if(configuration == null){
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Empty configuration");
        }

        return checkToken(userId, configuration.string(ValidToken.TOKEN), configuration.string(ValidToken.REFRESH_TOKEN), configuration.string(ValidToken.EXPIRATION_TIME));
    }

    public Json checkToken(String userId, String originalToken, String refreshToken, String expirationTime) throws EndpointException {
        String error = null;
        ErrorCode errorType = ErrorCode.API;
        boolean permanentException = true;

        if (StringUtils.isBlank(refreshToken)) {
            error = "Invalid refresh token: null";
            errorType = ErrorCode.ARGUMENT;
        }

        final ValidToken validToken = new ValidToken(originalToken, refreshToken, expirationTime);
        try {
            if (validToken.isExpired()) {
                try {
                    final GoogleRefreshTokenRequest request = new GoogleRefreshTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), new JacksonFactory(), validToken.getRefreshToken(), clientId, clientSecret);
                    validToken.updateToken(request.execute());
                    logger.info(String.format("Token regenerated for user [%s]", userId));
                } catch (HttpResponseException e) {
                    error = String.format("Error renewing the token [%s]", e.getContent() != null ? e.getContent() : e.getMessage());
                    errorType = ErrorCode.API;
                    if(e.getStatusCode() < 400 || e.getStatusCode() >= 500) {
                        permanentException = false;
                    }
                } catch (Exception e) {
                    error = String.format("Error renewing the token [%s]", e.getMessage());
                    errorType = ErrorCode.API;
                }
            }
        } catch (Exception e) {
            error = String.format("Error getting the token [%s]", e.getMessage());
            errorType = ErrorCode.API;
        }

        if(StringUtils.isBlank(error)){
            return validToken.toJson();
        } else {
            throw EndpointException.exception(errorType, error, permanentException);
        }
	}

    public void revokeTokens(String token, String refreshToken){
        try {
            String tokenToRevoke = refreshToken;
            if(StringUtils.isBlank(tokenToRevoke)){
                tokenToRevoke = token;
            }
            if(StringUtils.isNotBlank(tokenToRevoke)){
                final HttpRequestFactory factory = HTTP_TRANSPORT.createRequestFactory();
                final GenericUrl url = new GenericUrl("https://accounts.google.com/o/oauth2/revoke?token=" + tokenToRevoke);
                final HttpRequest request = factory.buildGetRequest(url);
                final HttpResponse response = request.execute();
                if(response.getStatusCode() != 200){
                    logger.warn(String.format("Error when try to revoke the Google tokens. Status [%s] [%s]", response.getStatusCode(), response.getStatusMessage()));
                }
            }
        } catch (Exception ex){
            logger.warn(String.format("Error when try to revoke the Google tokens [%s]", ex), ex);
        }
    }

    public GoogleAuthenticationService getAuthenticationService(String token) throws PermanentException {
        if (! this.services.contains(ServiceType.OAUTH_2)) {
            throw EndpointException.permanent(ErrorCode.CLIENT, "Authentication service (OAuth 2 API) was not defined as valid service");
        }
        return new GoogleAuthenticationService(application, token);
    }

    public GoogleCalendarService getService(String userId, String token, GoogleCalendarEndpoint endpoint) throws PermanentException {
        if (! this.services.contains(ServiceType.CALENDAR)) {
            throw EndpointException.permanent(ErrorCode.CLIENT, "Calendar service (Calendar API) was not defined as valid service");
        }
        return new GoogleCalendarService(userId, application, token, endpoint);
    }

    public OldGoogleCalendarService getOldService(String userId, String token, GoogleCalendarEndpoint endpoint) throws PermanentException {
        if (! this.services.contains(ServiceType.CALENDAR)) {
            throw EndpointException.permanent(ErrorCode.CLIENT, "Calendar service (Calendar API) was not defined as valid service");
        }
        return new OldGoogleCalendarService(userId, application, token, endpoint);
    }
}
