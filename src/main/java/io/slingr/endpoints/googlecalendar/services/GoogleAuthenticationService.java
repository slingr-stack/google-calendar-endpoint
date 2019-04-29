package io.slingr.endpoints.googlecalendar.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.googlecalendar.services.entities.ApiException;
import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>Service class that interacts with the Google Authentication APIs
 *
 * <p>Created by lefunes on 16/06/15.
 */
public class GoogleAuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthenticationService.class);

    private final Oauth2 service;

    public GoogleAuthenticationService(String applicationName, String token) {
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("Invalid token");
        }
        if (StringUtils.isBlank(applicationName)) {
            applicationName = "Google OAuth2";
        }

        final Oauth2 service;
        try {
            final NetHttpTransport nt = GoogleNetHttpTransport.newTrustedTransport();
            final JacksonFactory jf = new JacksonFactory();
            final GoogleCredential cd = new GoogleCredential().setAccessToken(token);

            service = new Oauth2.Builder(nt, jf, cd)
                    .setApplicationName(applicationName)
                    .build();
        } catch (HttpResponseException e) {
            logger.warn(String.format("Invalid response when try to build the authentication service [%s]", e.getContent() != null ? e.getContent() : e.getMessage()));
            throw ApiException.generate("Invalid response when try to build the authentication service", e);
        } catch (Exception e) {
            String cm = String.format("Error building the authentication service [%s]", e.getMessage());
            logger.warn(cm, e);
            throw ApiException.generate(cm, e);
        }
        this.service = service;
    }

    public Json getUserInformation() {
        try {
            Userinfoplus info = service.userinfo().get().execute();
            return Json.parse(info.toString());
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            logger.warn(String.format("Invalid response when try to get user information [%s]", e.getContent() != null ? e.getContent() : e.getMessage()), e);
            return ApiException.generate("Invalid response when try to get user information", e, true);
        } catch (IOException e) {
            String cm = String.format("Exception when execute request to get user information [%s]", e.getMessage());
            logger.warn(cm, e);
            return ApiException.generate(cm, e, true);
        }
    }
}
