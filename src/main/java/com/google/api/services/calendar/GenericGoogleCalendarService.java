package com.google.api.services.calendar;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson.JacksonFactory;
import io.slingr.endpoints.utils.Json;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Generic service over Google Calendar service
 * Created by lefunes on 18/04/17.
 */
public class GenericGoogleCalendarService extends com.google.api.services.calendar.Calendar {

    public GenericGoogleCalendarService(String applicationName, String token) throws GeneralSecurityException, IOException {
        super(new Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                new JacksonFactory(),
                new GoogleCredential().setAccessToken(token)
        ).setApplicationName(applicationName));
    }

    public GenericRequests generic() {
        return new GenericRequests();
    }

    public class GenericRequests {
        public GetRequest get(String url) throws IOException {
            GetRequest result = new GetRequest(url);
            initialize(result);
            return result;
        }

        public class GetRequest extends CalendarRequest<GenericJson> {
            GetRequest(String url) {
                super(GenericGoogleCalendarService.this, "GET", url, null, GenericJson.class);
            }

            @Override
            public GetRequest set(String parameterName, Object value) {
                return (GetRequest) super.set(parameterName, value);
            }
        }

        public PostRequest post(String url, Json content) throws IOException {
            PostRequest result = new PostRequest(url, content);
            initialize(result);
            return result;
        }

        public class PostRequest extends CalendarRequest<GenericJson> {
            PostRequest(String url, Json content) {
                super(GenericGoogleCalendarService.this, "POST", url, content != null ? content.toMap() : null, GenericJson.class);
            }
        }

        public PutRequest put(String url, Json content) throws IOException {
            PutRequest result = new PutRequest(url, content);
            initialize(result);
            return result;
        }

        public class PutRequest extends CalendarRequest<GenericJson> {
            PutRequest(String url, Json content) {
                super(GenericGoogleCalendarService.this, "PUT", url, content != null ? content.toMap() : null, GenericJson.class);
            }
        }

        public PatchRequest patch(String url, Json content) throws IOException {
            PatchRequest result = new PatchRequest(url, content);
            initialize(result);
            return result;
        }

        public class PatchRequest extends CalendarRequest<GenericJson> {
            PatchRequest(String url, Json content) {
                super(GenericGoogleCalendarService.this, "PATCH", url, content != null ? content.toMap() : null, GenericJson.class);
            }
        }

        public DeleteRequest delete(String url) throws IOException {
            DeleteRequest result = new DeleteRequest(url);
            initialize(result);
            return result;
        }

        public class DeleteRequest extends CalendarRequest<GenericJson> {
            DeleteRequest(String url) {
                super(GenericGoogleCalendarService.this, "DELETE", url, null, GenericJson.class);
            }
        }
    }

}
