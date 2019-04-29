package io.slingr.endpoints.googlecalendar.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.*;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.googlecalendar.GoogleCalendarEndpoint;
import io.slingr.endpoints.googlecalendar.services.entities.ApiException;
import io.slingr.endpoints.googlecalendar.services.entities.GCCalendar;
import io.slingr.endpoints.googlecalendar.services.entities.GCEvent;
import io.slingr.endpoints.googlecalendar.services.utils.DateTimeUtils;
import io.slingr.endpoints.services.exchange.Parameter;
import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>Service class that interacts with the Google Calendar API
 *
 * <p>Created by lefunes on 16/06/15.
 */
public class OldGoogleCalendarService {
    private static final Logger logger = Logger.getLogger(OldGoogleCalendarService.class);

    private final String userId;
    private final com.google.api.services.calendar.Calendar service;
    private final GoogleCalendarEndpoint endpoint;

    public OldGoogleCalendarService(String userId, String applicationName, String token, GoogleCalendarEndpoint endpoint) {
        this.userId = userId;
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("Invalid token");
        }
        if (StringUtils.isBlank(applicationName)) {
            applicationName = "Google Calendar";
        }

        final com.google.api.services.calendar.Calendar service;
        try {
            final NetHttpTransport nt = GoogleNetHttpTransport.newTrustedTransport();
            final JacksonFactory jf = new JacksonFactory();
            final GoogleCredential cd = new GoogleCredential().setAccessToken(token);

            service = new com.google.api.services.calendar.Calendar.Builder(nt, jf, cd)
                    .setApplicationName(applicationName)
                    .build();
        } catch (HttpResponseException e) {
            logger.info(String.format("Invalid response when try to build the Google Calendar client [%s]", e.getContent() != null ? e.getContent() : e.getMessage()));
            throw ApiException.generate("Invalid response when try to build the Google Calendar client", e);
        } catch (Exception e) {
            String cm = String.format("Error building the calendar service [%s]", e.getMessage());
            logger.info(cm, e);
            throw ApiException.generate(cm, e);
        }
        this.service = service;
        this.endpoint = endpoint;
    }

    public Json calendarList(String functionId) {
        try {
            boolean processedResults = false;
            List<CalendarListEntry> list = new ArrayList<>();
            String pageToken = null;
            do {
                CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
                if (calendarList != null) {
                    if (calendarList.getItems() != null) {
                        list.addAll(calendarList.getItems());
                        processedResults = true;
                    }
                    pageToken = calendarList.getNextPageToken();
                } else {
                    pageToken = null;
                }
            } while (StringUtils.isNotBlank(pageToken));

            if (processedResults) {
                final Json result = Json.map();

                final List<Json> cls = new ArrayList<>();
                for (CalendarListEntry calendarListEntry : list) {
                    final GCCalendar calendar = GCCalendar.fromGoogle(calendarListEntry);
                    cls.add(calendar.toJson());
                }
                result.set("calendars", cls);

                return result;
            } else {
                logger.info("Invalid response: CalendarList.list");
            }
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e, "Invalid response when try to get the calendar list");
        } catch (IOException e) {
            return processException(e);
        }
        return null;
    }

    public Json createCalendar(String summary, String description, String location, String timezone, Json data, String functionId) {
        try {
            final GCCalendar calendar = new GCCalendar(summary);
            calendar.setDescription(description);
            calendar.setLocation(location);
            calendar.setTimezone(timezone);
            calendar.setData(data);

            logger.info(String.format("Calendar [%s]", calendar.toJson()));

            Calendar createdCalendar = service.calendars().insert(calendar.toGoogle()).execute();

            logger.info(String.format("Created calendar [%s]", Json.fromMap(createdCalendar).toString()));

            final GCCalendar cCalendar = GCCalendar.fromGoogle(createdCalendar);
            return cCalendar.toJson();
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json patchCalendar(String calendarId, String summary, String description, String location, String timezone, Json data, String functionId) {
        try {
            final GCCalendar calendar = new GCCalendar(summary);
            calendar.setId(calendarId);
            calendar.setDescription(description);
            calendar.setLocation(location);
            calendar.setTimezone(timezone);
            calendar.setData(data);

            logger.info(String.format("Calendar [%s]", calendar.toJson()));

            Calendar updatedCalendar = service.calendars().patch(calendarId, calendar.toGoogle()).execute();

            logger.info(String.format("Updated calendar [%s]", Json.fromMap(updatedCalendar).toString()));

            final GCCalendar cCalendar = GCCalendar.fromGoogle(updatedCalendar);
            return cCalendar.toJson();
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json removeCalendar(String calendarId, String functionId) {
        Json response = Json.map();
        response.set("result", "error");  // deprecated field
        response.set("deleted", false);
        response.set("calendarId", calendarId);

        try {
            logger.info(String.format("Delete calendar [%s]", calendarId));

            service.calendars().delete(calendarId).execute();

            logger.info("Calendar deleted");

            response.set("result", "ok");
            response.set("deleted", true);
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
        return response;
    }

    public Json clearCalendar(String calendarId, String functionId) {
        Json response = Json.map();
        response.set("result", "error");  // deprecated field
        response.set("cleared", false);
        response.set("calendarId", calendarId);

        try {
            logger.info(String.format("Clear calendar [%s]", calendarId));

            service.calendars().clear(calendarId).execute();

            logger.info("Calendar cleared");

            response.set("result", "ok");
            response.set("cleared", true);
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
        return response;
    }

    public Json eventsList(String calendarId, String query, Object from, Object to, String timezone, String pageToken, Integer maxResults, String queryToken, Json data, String functionId) {
        Json response = Json.map();
        response.set("calendarId", calendarId);

        try {
            final Json events = internalEventsList(calendarId, query, from, to, timezone, pageToken, maxResults, queryToken, data, functionId);
            if(events != null && !events.isEmpty()){
                response = events;
                logger.info(String.format("Event list [%s]", response.toString()));
            }
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (Exception e) {
            return processException(e);
        }
        return response;
    }

    public Json eventsSync(String calendarId, String queryToken, String functionId) {
        Json response = Json.map();
        response.set("calendarId", calendarId);

        try {
            final FullEventListBuilder builder;
            if(StringUtils.isBlank(queryToken)){
                // full sync
                builder = new FullEventListBuilder(calendarId, functionId);
            } else {
                // perform incremental sync
                builder = new FullEventListBuilder(calendarId, queryToken, functionId);
            }

            response = builder.execute();
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (Exception e) {
            return processException(e);
        }
        return response;
    }

    public Json createEvent(String calendarId, Object start, Object end, Boolean allDayEvent, String timezone, String summary,String description, String location, Json data, String functionId) {
        Json response = Json.map();
        response.set("calendarId", calendarId);

        try {
            final GCEvent event = new GCEvent(calendarId, start, end, timezone);
            event.setAllDayEvent(allDayEvent);
            event.setSummary(summary);
            event.setDescription(description);
            event.setLocation(location);
            event.setData(data);

            logger.info(String.format("Event [%s]", event.toJson()));

            Event createdEvent = service.events().insert(calendarId, event.toGoogle()).execute();

            logger.info(String.format("Created event [%s]", Json.fromMap(createdEvent).toString()));

            final GCEvent cEvent = GCEvent.fromGoogle(calendarId, createdEvent);
            response = cEvent.toJson();
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
        return response;
    }

    public Json patchEvent(String eventId, String calendarId, Object start, Object end, Boolean allDayEvent, String timezone, String summary,String description, String location, Json data, String functionId) {
        Json response = Json.map();
        response.set("calendarId", calendarId);

        try {
            final GCEvent event = new GCEvent(calendarId, start, end, timezone);
            event.setId(eventId);
            event.setAllDayEvent(allDayEvent);
            event.setSummary(summary);
            event.setDescription(description);
            event.setLocation(location);
            event.setData(data);

            logger.info(String.format("Event [%s]", event.toJson()));

            Event updatedEvent = service.events().patch(calendarId, eventId, event.toGoogle()).execute();

            logger.info(String.format("Updated event [%s]", Json.fromMap(updatedEvent).toString()));

            final GCEvent cEvent = GCEvent.fromGoogle(calendarId, updatedEvent);
            response = cEvent.toJson();
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
        return response;
    }

    public Json removeEvent(String calendarId, String eventId, String functionId) {
        Json response = Json.map();
        response.set("result", "error");  // deprecated field
        response.set("deleted", false);
        response.set("calendarId", calendarId);
        response.set("eventId", eventId);

        try {
            logger.info(String.format("Delete event [%s]", eventId));

            service.events().delete(calendarId, eventId).execute();

            logger.info("Event deleted");

            response.set("result", "ok");
            response.set("deleted", true);
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
        return response;
    }

    private class FullEventListBuilder {
        static final int DEFAULT_MAX_RESULTS = 2500;
        static final int MAX_ERROR_COUNT = 2;

        private final String calendarId;
        private final String query;
        private final Object from;
        private final Object to;
        private final String timezone;
        private final String initialPageToken;
        private final String initialQueryToken;
        private final Json data;
        private int errorCount;
        private String functionId;

        FullEventListBuilder(String calendarId, String query, Object from, Object to, String timezone, String pageToken, String queryToken, Json data, String functionId) {
            this.calendarId = calendarId;
            this.query = query;
            this.from = from;
            this.to = to;
            this.timezone = timezone;
            this.initialPageToken = pageToken;
            this.initialQueryToken = queryToken;
            this.data = data;
            this. functionId = functionId;
            this.errorCount = 0;
        }

        /**
         * Full initial synchronization
         */
        FullEventListBuilder(String calendarId, String functionId) {
            this(calendarId, null, new Date().getTime(), null, "UTC", null, null, null, functionId);
        }

        /**
         * Incremental synchronization
         */
        FullEventListBuilder(String calendarId, String queryToken, String functionId) {
            this(calendarId, null, null, null, null, null, queryToken, null, functionId);
        }

        Json execute() {
            final Json response = Json.map();
            response.set("result", "error");
            response.set("calendarId", calendarId);

            try {
                try {
                    boolean processedResults = false;
                    final List<Json> eventList = new ArrayList<>();
                    String pageToken = this.initialPageToken;
                    String queryToken = null;
                    Json lastInfo = null;
                    int maxPages = 10;
                    do {
                        final Json partialResult = internalEventsList(calendarId, query, from, to, timezone, pageToken, DEFAULT_MAX_RESULTS, initialQueryToken, data, functionId);
                        if (partialResult != null) {
                            if (partialResult.object("events") != null) {
                                eventList.addAll(partialResult.jsons("events"));
                                processedResults = true;
                            }
                            if (partialResult.object("info") != null) {
                                lastInfo = partialResult.json("info");
                            }
                            pageToken = partialResult.string("nextPageToken");
                            queryToken = partialResult.string("queryToken");
                        } else {
                            pageToken = null;
                        }
                        maxPages--;
                    } while (StringUtils.isNotBlank(pageToken) && maxPages>=0);

                    if (processedResults) {
                        response.set("result", "ok");
                        response.set("events", eventList);
                        response.set("info", lastInfo);
                        response.set("queryToken", queryToken);
                        return response;
                    } else {
                        logger.info("Invalid response: CalendarList.list");
                    }
                } catch (EndpointException e) {
                    throw e;
                } catch (Exception e) {
                    return processException(e);
                }
            } catch (EndpointException e) {
                Json error = e.toJson(true);

                boolean fullSyncRequired = false;
                if(errorCount < MAX_ERROR_COUNT) {
                    Json description = error.json(Parameter.EXCEPTION_ADDITIONAL_INFO).json("googleException");

                    if(description != null) {
                        // A 410 status code, "Gone", indicates that the sync token is invalid.
                        if ("410".equals(description.string("code"))) {
                            fullSyncRequired = true;
                        } else if (StringUtils.isNotBlank(description.string("message")) && description.string("message").contains("ync token")) {
                            fullSyncRequired = true;
                        }
                    }
                }

                if(fullSyncRequired) {
                    // when there is an error, perform full synchronization
                    FullEventListBuilder builder = new FullEventListBuilder(calendarId, functionId);
                    builder.errorCount = this.errorCount + 1;
                    return builder.execute();
                } else {
                    return e.toJson(true);
                }
            }

            return response;
        }
    }

    private Json internalEventsList(String calendarId, String query, Object from, Object to, String timezone, String pageToken, Integer maxResults, String queryToken, Json data, String functionId) throws EndpointException {
        final Json response = Json.map();
        response.set("result", "error");  // deprecated field
        response.set("calendarId", calendarId);

        try {
            final com.google.api.services.calendar.Calendar.Events.List list = service.events().list(calendarId);

            if (data != null && !data.isEmpty()) {
                for (String key : data.keys()) {
                    list.set(key, data.object(key));
                }
            }
            if (StringUtils.isNotBlank(pageToken)) {
                list.setPageToken(pageToken);
            }
            if (maxResults != null) {
                list.setMaxResults(maxResults);
            }

            if (StringUtils.isNotBlank(queryToken)) {
                list.setSyncToken(queryToken);
            } else {
                if (StringUtils.isNotBlank(query)) {
                    list.setQ(query);
                }
                if (from != null) {
                    DateTime dt = DateTimeUtils.getDateTime(from, timezone);
                    if(dt != null){
                        if (dt.isDateOnly()) {
                            dt = new DateTime(false, dt.getValue() - 5, dt.getTimeZoneShift());
                        }
                        list.setTimeMin(dt);
                    }
                }
                if (to != null) {
                    DateTime dt = DateTimeUtils.getDateTime(to, timezone);
                    if(dt != null) {
                        if (dt.isDateOnly()) {
                            dt = new DateTime(false, dt.getValue() + 5, dt.getTimeZoneShift());
                        }
                        list.setTimeMax(dt);
                    }
                }
            }

            logger.info(String.format("Event list request [%s]", Json.fromMap(list).toString()));

            final Events events = list.execute();

            logger.info(String.format("Event list response [%s]", Json.fromMap(events).toString()));

            // items
            final List<Json> eventsList = new ArrayList<>();
            List<Event> items = events.getItems();
            if (items != null) {
                for (Event googleEvent : items) {
                    final GCEvent event = GCEvent.fromGoogle(calendarId, googleEvent);
                    eventsList.add(event.toJson());
                }
            }
            response.set("events", eventsList);

            // next page token
            if (StringUtils.isNotBlank(events.getNextPageToken())) {
                response.set("nextPageToken", events.getNextPageToken());
            }

            // next sync token
            if (StringUtils.isNotBlank(events.getNextSyncToken())) {
                response.set("queryToken", events.getNextSyncToken());
            }

            // info
            final Json info = Json.map();
            events.keySet().stream()
                    .filter(key -> !key.equals("items") && !key.equals("nextPageToken") && !key.equals("nextSyncToken"))
                    .forEach(key -> info.set(key, events.get(key)));

            response.set("info", info);

            response.set("result", "ok");
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e, "Invalid response when try to process the event list");
        } catch (IOException e) {
            throw EndpointException.retryable(ErrorCode.API, "Exception when try to get the event list", e);
        }
        return response;
    }

    private static Json processException(Exception e) {
        return GoogleCalendarService.processException(logger, e);
    }

    private static Json processException(Exception e, String text) {
        return GoogleCalendarService.processException(logger, e, text);
    }

    private Json processHttpResponseException(String functionId, HttpResponseException e) {
        return GoogleCalendarService.processHttpResponseException(endpoint, userId, functionId, e);
    }

    private Json processHttpResponseException(String functionId, HttpResponseException e, String text) {
        return GoogleCalendarService.processHttpResponseException(endpoint, userId, functionId, e, text);
    }
}