package io.slingr.endpoints.googlecalendar.services;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.GenericGoogleCalendarService;
import com.google.api.services.calendar.model.*;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.googlecalendar.GoogleCalendarEndpoint;
import io.slingr.endpoints.googlecalendar.services.entities.ApiException;
import io.slingr.endpoints.googlecalendar.services.utils.DateTimeUtils;
import io.slingr.endpoints.services.exchange.Parameter;
import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>Service class that interacts with the Google Calendar API
 *
 * <p>Created by lefunes on 16/06/15.
 */
public class GoogleCalendarService {
    private static final Logger logger = Logger.getLogger(GoogleCalendarService.class);

    public static final String EXPIRATION_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(EXPIRATION_TIME_FORMAT);

    private final String userId;
    private final GenericGoogleCalendarService service;
    private final GoogleCalendarEndpoint endpoint;

    public GoogleCalendarService(String userId, String applicationName, String token, GoogleCalendarEndpoint endpoint) {
        this.userId = userId;
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("Invalid token");
        }
        if (StringUtils.isBlank(applicationName)) {
            applicationName = "Google Calendar";
        }

        final GenericGoogleCalendarService service;
        try {
            service = new GenericGoogleCalendarService(applicationName, token);
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

    private String checkCalendarId(String value, Json options){
        return checkId("calendarId", value, options);
    }

    private String checkEventId(String value, Json options){
        return checkId("eventId", value, options);
    }

    private String checkId(String name, String value, Json options){
        if(options == null){
            return value;
        } else {
            if(StringUtils.isNotBlank(value)){
                return value;
            } else {
                return options.string(name);
            }
        }
    }

    public Json findOneCalendar(String calendarId, Json options, String functionId) {
        try {
            calendarId = checkCalendarId(calendarId, options);
            logger.info(String.format("Calendar id [%s]", calendarId));

            final Calendar calendar = service.calendars().get(calendarId).execute();
            final Json response = getJson(calendar);

            logger.info(String.format("Calendar found [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json findCalendars(Json params, String functionId) {
        try {
            logger.info(String.format("Get calendars [%s]", params));

            final CalendarList calendarList = calendarQuery(params).execute();
            final Json response = getJson(calendarList);

            logger.info(String.format("Calendars found [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json createCalendar(Json calendar, String functionId) {
        try {
            final Calendar c = fillCalendar(calendar);

            final Calendar createdCalendar = service.calendars().insert(c).execute();
            final Json response = getJson(createdCalendar);

            logger.info(String.format("Created calendar [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json updateCalendar(Json calendar, String functionId) {
        try {
            final Calendar c = fillCalendar(calendar);

            final Calendar updatedCalendar = service.calendars().update(c.getId(), c).execute();
            final Json response = getJson(updatedCalendar);

            logger.info(String.format("Updated calendar [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json deleteCalendar(String calendarId, Json options, String functionId) {
        calendarId = checkCalendarId(calendarId, options);
        logger.info(String.format("Delete calendar [%s]", calendarId));
        final Json calendar = findOneCalendar(calendarId, options, functionId);
        try {
            if(calendar.bool("primary", false)){
                // primary calendar -> clear
                logger.info(String.format("Clear primary calendar [%s]", calendarId));
                service.calendars().clear(calendarId).execute();
                logger.info("Calendar cleared");
            } else {
                // secondary calendar -> delete
                logger.info(String.format("Delete secondary calendar [%s]", calendarId));
                service.calendars().delete(calendarId).execute();
                logger.info("Calendar deleted");
            }
            return calendar;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json findOneEvent(String calendarId, String eventId, Json options, String functionId) {
        try {
            calendarId = checkCalendarId(calendarId, options);
            eventId = checkEventId(eventId, options);
            logger.info(String.format("Event id [%s][%s]", eventId, calendarId));

            final Event event = service.events().get(calendarId, eventId).execute();
            final Json response = getJson(event);

            logger.info(String.format("Event found [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json findEvents(String calendarId, Json params, String functionId) {
        try {
            calendarId = checkCalendarId(calendarId, params);
            logger.info(String.format("Get events [%s]", params));

            final Events events = eventsQuery(calendarId, params).execute();
            final Json response = getJson(events);

            logger.info(String.format("Events found [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json createEvent(String calendarId, Json event, String functionId) {
        try {
            calendarId = checkCalendarId(calendarId, event);

            final String url = String.format("https://www.googleapis.com/calendar/v3/calendars/%s/events", calendarId);

            final Json response = postRequest(url, event, functionId);
            logger.info(String.format("Created event [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json updateEvent(String calendarId, String eventId, Json event, String functionId) {
        try {
            calendarId = checkCalendarId(calendarId, event);
            eventId = checkEventId(eventId, event);

            final String url = String.format("https://www.googleapis.com/calendar/v3/calendars/%s/events/%s", calendarId, eventId);

            final Json response = putRequest(url, event, functionId);
            logger.info(String.format("Updated event [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json deleteEvent(String calendarId, String eventId, Json options, String functionId) {
        calendarId = checkCalendarId(calendarId, options);
        eventId = checkEventId(eventId, options);
        logger.info(String.format("Delete event [%s] on calendar [%s]", eventId, calendarId));
        final Json event = findOneEvent(calendarId, eventId, options, functionId);
        try {
            service.events().delete(calendarId, eventId).execute();
            logger.info("Event deleted");
            return event;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    private com.google.api.services.calendar.Calendar.CalendarList.List calendarQuery(Json params) throws IOException {
        final com.google.api.services.calendar.Calendar.CalendarList.List cList = service.calendarList().list();
        if(params != null) {
            for (String key : params.keys()) {
                cList.set(key, params.object(key));
            }
        }
        return cList;
    }

    private com.google.api.services.calendar.Calendar.Events.List eventsQuery(String calendarId, Json params) throws IOException {
        final com.google.api.services.calendar.Calendar.Events.List cList = service.events().list(calendarId);
        if(params != null) {
            for (String key : params.keys()) {
                if ("timeMin".equals(key) || "timeMax".equals(key) || "updatedMin".equals(key)) {
                    cList.set(key, new Date(params.longInteger(key)));
                } else {
                    cList.set(key, params.object(key));
                }
            }
        }
        return cList;
    }

    private Calendar fillCalendar(Json calendar) {
        final Calendar c = new Calendar();
        fillJson(c, calendar, "Calendar");
        if(StringUtils.isBlank(c.getId()) && StringUtils.isNotBlank((String) c.get("calendarId"))){
            c.setId((String) c.get("calendarId"));
        }
        return c;
    }

    public Json getJson(GenericJson genericJson) {
        final Json response = Json.fromMap(genericJson);
        response.traverse(new Json.Visitor() {
            @Override
            public Object convertValue(String key, Object value, String path) {
                if(value instanceof DateTime){
                    return ((DateTime) value).toStringRfc3339();
                }
                return value;
            }
        });
        return response;
    }

    private void fillJson(GenericJson json, Json fromAppJson, String name) {
        if(json != null && fromAppJson != null) {
            logger.info(String.format("%s [%s]", name, fromAppJson));
            fromAppJson.toMap().forEach((key, value) -> setField(json, key, value));
        } else {
            logger.info(String.format("%s [-]", name));
        }
    }

    public void setField(GenericJson json, String key, Object value){
        if(value instanceof Json || value instanceof Map){
            final Field field = json.getClassInfo().getField(key);
            if(field != null) {
                setField(field, value);
            } else {
                json.set(key, value);
            }
        } else {
            json.set(key, value);
        }
    }

    public void setField(Field field, Object value){
        if(field != null) {
            final Json jsonValue = Json.fromObject(value);
            jsonValue.forEachMap((k, v) -> {
                try {
                    field.set(k, v);
                } catch (Exception ex) {
                    logger.info(String.format("Exception when try to set field [%s]: %s", field.getName(), ex.getMessage()), ex);
                }
            });
        }
    }

    public Json getRequest(String url, String functionId) {
        try {
            final GenericJson json = service.generic().get(url).execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json postRequest(String url, Json content, String functionId) {
        try {
            final GenericJson json = service.generic().post(url, content).execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json putRequest(String url, Json content, String functionId) {
        try {
            final GenericJson json = service.generic().put(url, content).execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json patchRequest(String url, Json content, String functionId) {
        try {
            final GenericJson json = service.generic().patch(url, content).execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json deleteRequest(String url, String functionId) {
        try {
            final GenericJson json = service.generic().delete(url).execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
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
            this.functionId = functionId;
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
                    eventsList.add(getJson(googleEvent).set("calendarId", calendarId));
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
            endpoint.checkDisconnection(userId, e, functionId);
            throw ApiException.generate(String.format("Invalid response when try to process the event list [%s]", e.getContent() != null ? e.getContent() : e.getMessage()), e);
        } catch (IOException e) {
            throw EndpointException.retryable(ErrorCode.API, "Exception when try to get the event list", e);
        }
        return response;
    }

    public Json findAllCalendars() {
        try {
            boolean processedResults = false;
            List<CalendarListEntry> list = new ArrayList<>();
            String pageToken = null;
            do {
                final com.google.api.services.calendar.Calendar.CalendarList.List cList = service.calendarList().list();
                if(StringUtils.isNotBlank(pageToken)) {
                    cList.setPageToken(pageToken);
                }
                final CalendarList calendarList = cList.execute();
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
                return Json.map()
                        .set("calendars", list.stream()
                                .map(Json::fromMap)
                                .collect(Collectors.toList())
                        );
            } else {
                logger.info("Invalid response: CalendarList.list");
            }
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            endpoint.checkDisconnection(userId, e, null);
            logger.info(String.format("Invalid response when try to get the calendar list [%s]", e.getContent() != null ? e.getContent() : e.getMessage()));
            return ApiException.generate("Invalid response when try to get the calendar list", e, true);
        } catch (IOException e) {
            return processException(e);
        }
        return null;
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

    private static Json processException(Exception e) {
        return processException(logger, e);
    }

    private static Json processException(Exception e, String text) {
        return processException(logger, e, text);
    }

    public static Json processException(Logger logger, Exception e) {
        return processException(logger, e, "Exception when execute request");
    }

    public static Json processException(Logger logger, Exception e, String text) {
        final String message = String.format("%s [%s]", text, e.getMessage());
        logger.info(message, e);
        return ApiException.generate(message, e, true);
    }

    private Json processHttpResponseException(String functionId, HttpResponseException e) {
        return processHttpResponseException(endpoint, userId, functionId, e);
    }

    private Json processHttpResponseException(String functionId, HttpResponseException e, String text) {
        return processHttpResponseException(endpoint, userId, functionId, e, text);
    }

    public static Json processHttpResponseException(GoogleCalendarEndpoint endpoint, String userId, String functionId, HttpResponseException e) {
        return processHttpResponseException(endpoint, userId, functionId, e, "Exception when execute request");
    }

    public static Json processHttpResponseException(GoogleCalendarEndpoint endpoint, String userId, String functionId, HttpResponseException e, String text) {
        endpoint.checkDisconnection(userId, e, functionId);

        String message;
        try {
            Json json = Json.fromObject(e.getContent() != null ? e.getContent() : e.getMessage());
            message = String.format("%s [%s]", text, (json.contains("message") ? json.string("message") : json.toString()));
        } catch (Exception ex){
            message = String.format("%s [%s]", text, e.getContent() != null ? e.getContent() : e.getMessage());
        }
        logger.info(message, e);
        return ApiException.generate(message, e, true);
    }
}
