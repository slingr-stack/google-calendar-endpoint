package io.slingr.endpoints.googlecalendar.services.entities;

import com.google.api.client.util.Data;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import io.slingr.endpoints.googlecalendar.services.utils.DateTimeUtils;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.converters.JsonSource;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>Representation of an event on the application side.
 *
 * <p>Created by lefunes on 10/11/14.
 */
public class GCEvent implements JsonSource {
    /**
     * Identifier of the event.
     */
    private String id;
    /**
     * Calendar identifier.
     */
    private String calendarId;
    /**
     * The (inclusive) start time of the event. For a recurring event, this is the start time of the first instance.
     */
    private String start;
    private String startDate;
    /**
     * The (exclusive) end time of the event. For a recurring event, this is the end time of the first instance.
     */
    private String end;
    private String endDate;
    /**
     * True if the event is an All-Day event
     */
    private boolean allDayEvent = false;
    /**
     * Title of the event.
     */
    private String summary;
    /**
     * Description of the event. Optional.
     */
    private String description;
    /**
     * Geographic location of the event as free-form text. Optional.
     */
    private String location;
    /**
     * Other options: https://developers.google.com/google-apps/calendar/v3/reference/events/insert
     */
    private Json data = Json.map();
    /**
     * True if the event was cancelled
     */
    private boolean cancelled;

    public GCEvent(String calendarId, Object start, Object end, String timezone) {
        this(calendarId, false);
        setStart(start, timezone);
        setEnd(end, timezone);
    }

    public GCEvent(String calendarId, boolean cancelled) {
        this.calendarId = calendarId;
        this.cancelled = cancelled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public String getStart() {
        return start;
    }

    public void setStart(Object start, String timezone) {
        DateTime dt = DateTimeUtils.getDateTime(start, timezone);
        if(dt != null) {
            this.start = dt.toStringRfc3339();
            this.startDate = DateTimeUtils.getOnlyDate(this.start);
        }
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(Object end, String timezone) {
        DateTime dt = DateTimeUtils.getDateTime(end, timezone);
        if(dt != null) {
            this.end = dt.toStringRfc3339();
            this.endDate = DateTimeUtils.getOnlyDate(this.end);
        }
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Json getData() {
        if (data == null) {
            data = Json.map();
        }
        return data;
    }

    public void setData(Json data) {
        this.data = data;
    }

    public boolean isAllDayEvent() {
        return allDayEvent;
    }

    public void setAllDayEvent(Boolean allDayEvent) {
        if(allDayEvent != null) {
            this.allDayEvent = allDayEvent;
        } else {
            this.allDayEvent = false;
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Event toGoogle() {
        final Event event = new Event();

        if(!isCancelled()) {
            if (allDayEvent) {
                EventDateTime edt = new EventDateTime();
                edt.setDate(new DateTime(startDate));
                edt.setDateTime(Data.NULL_DATE_TIME);
                event.setStart(edt);

                edt = new EventDateTime();
                edt.setDate(new DateTime(endDate));
                edt.setDateTime(Data.NULL_DATE_TIME);
                event.setEnd(edt);
            } else {
                EventDateTime edt = new EventDateTime();
                edt.setDate(Data.NULL_DATE_TIME);
                edt.setDateTime(new DateTime(start));
                event.setStart(edt);

                edt = new EventDateTime();
                edt.setDate(Data.NULL_DATE_TIME);
                edt.setDateTime(new DateTime(end));
                event.setEnd(edt);
            }
        }

        if (data != null) {
            event.putAll(data.toMap());
        }

        if(!isCancelled()) {
            if (StringUtils.isNotBlank(location)) {
                event.setLocation(location);
            }
            if (StringUtils.isNotBlank(description)) {
                event.setDescription(description);
            }
            if (StringUtils.isNotBlank(summary)) {
                event.setSummary(summary);
            }
        } else {
            event.setStatus("cancelled");
        }

        if (StringUtils.isNotBlank(id)) {
            event.setId(id);
        }

        return event;
    }

    public static GCEvent fromGoogle(String calendarId, Event googleEvent) {
        if (googleEvent == null) {
            return null;
        }
        GCEvent event;
        if(googleEvent.getStatus() == null || !googleEvent.getStatus().equalsIgnoreCase("cancelled")) {
            if (googleEvent.getStart() == null || googleEvent.getEnd() == null) {
                return null;
            }

            if (googleEvent.getStart().getDate() != null && googleEvent.getStart().getDate().getValue() > 1) {
                event = new GCEvent(calendarId, googleEvent.getStart().getDate(), googleEvent.getEnd().getDate(), googleEvent.getEnd().getTimeZone());
                event.setAllDayEvent(true);
            } else {
                event = new GCEvent(calendarId, googleEvent.getStart().getDateTime(), googleEvent.getEnd().getDateTime(), googleEvent.getEnd().getTimeZone());
                event.setAllDayEvent(false);
            }
            if (StringUtils.isNotBlank(googleEvent.getSummary())) {
                event.setSummary(googleEvent.getSummary());
            }
            if (StringUtils.isNotBlank(googleEvent.getDescription())) {
                event.setDescription(googleEvent.getDescription());
            }
            if (StringUtils.isNotBlank(googleEvent.getLocation())) {
                event.setLocation(googleEvent.getLocation());
            }
        } else {
            //cancelled event
            event = new GCEvent(calendarId, true);
        }

        if (StringUtils.isNotBlank(googleEvent.getId())) {
            event.setId(googleEvent.getId());
        }
        event.setData(Json.fromMap(googleEvent));

        return event;
    }

    @Override
    public Json toJson() {
        Json json = Json.map();
        if (data == null) {
            data = Json.map();
        }
        json.set("data", data);

        if (StringUtils.isNotBlank(id)) {
            json.set("eventId", id);
        } else if (data != null) {
            json.set("eventId", data.string("eventId"));
        }
        if (StringUtils.isNotBlank(calendarId)) {
            json.set("calendarId", calendarId);
        } else if (data != null) {
            json.set("calendarId", data.string("calendarId"));
        }
        if(!isCancelled()) {
            if (StringUtils.isNotBlank(location)) {
                json.set("location", location);
            } else if (data != null) {
                json.set("location", data.string("location"));
            }
            if (StringUtils.isNotBlank(description)) {
                json.set("description", description);
            } else if (data != null) {
                json.set("description", data.string("description"));
            }
            if (StringUtils.isNotBlank(summary)) {
                json.set("summary", summary);
            } else if (data != null) {
                json.set("summary", data.string("summary"));
            }

            json.set("allDayEvent", allDayEvent);
            json.set("start", start);
            json.set("startDate", startDate);
            if (start == null && data != null && data.json("start") != null) {
                Json startMap = data.json("start");
                if (startMap.string("date") != null) {
                    json.set("startDate", startMap.string("date"));
                    if (startMap.string("timeZone") != null) {
                        json.set("timezone", startMap.string("timeZone"));
                    }
                }
                if (startMap.string("datetime") != null) {
                    json.set("start", startMap.string("datetime"));
                    if (startMap.string("timeZone") != null) {
                        json.set("timezone", startMap.string("timeZone"));
                    }
                }
            }
            json.set("end", end);
            json.set("endDate", endDate);
            if (start == null && data != null && data.json("end") != null) {
                Json endMap = data.json("end");
                if (endMap.string("date") != null) {
                    json.set("endDate", endMap.string("date"));
                    if (endMap.string("timeZone") != null) {
                        json.set("timezone", endMap.string("timeZone"));
                    }
                }
                if (endMap.string("datetime") != null) {
                    json.set("end", endMap.string("datetime"));
                    if (endMap.string("timeZone") != null) {
                        json.set("timezone", endMap.string("timeZone"));
                    }
                }
            }
        } else {
            json.set("cancelled", true);
        }

        return json;
    }

    public static GCEvent fromJson(String calendarId, Json json) {
        if (StringUtils.isBlank(calendarId) || json == null) {
            return null;
        }
        GCEvent event;

        if(json.string("status") == null || !json.string("status").equalsIgnoreCase("cancelled")) {
            event = new GCEvent(calendarId, json.object("start"), json.object("end"), json.string("timezone"));
            if (json.object("allDayEvent") != null) {
                event.setAllDayEvent(json.bool("allDayEvent", false));
            }
            if (StringUtils.isNotBlank(json.string("summary"))) {
                event.setSummary(json.string("summary"));
            }
            if (StringUtils.isNotBlank(json.string("description"))) {
                event.setDescription(json.string("description"));
            }
            if (StringUtils.isNotBlank(json.string("location"))) {
                event.setLocation(json.string("location"));
            }
        } else {
            event = new GCEvent(calendarId, true);
        }
        if (StringUtils.isNotBlank(json.string("eventId"))) {
            event.setId(json.string("eventId"));
        }
        event.setData(json.json("data"));

        return event;
    }
}
