package io.slingr.endpoints.googlecalendar.services.entities;

import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.converters.JsonSource;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>Representation of a calendar on application side
 *
 * <p>Created by lefunes on 26/11/14.
 */
public class GCCalendar implements JsonSource {
    /**
     * Identifier of the calendar.
     */
    private String id;
    /**
     * Title of the calendar.
     */
    private String summary;
    /**
     * Description of the calendar. Optional.
     */
    private String description;
    /**
     * Geographic location of the calendar as free-form text. Optional.
     */
    private String location;
    /**
     * The time zone of the calendar. Optional..
     */
    private String timezone;
    private boolean deleted = false;
    /**
     * Other options: https://developers.google.com/google-apps/calendar/v3/reference/calendars/insert
     */
    private Json data = Json.map();

    public GCCalendar(String summary) {
        this.summary = summary;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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

    public Calendar toGoogle() {
        final Calendar calendar = new Calendar();
        calendar.setSummary(summary);

        if (data != null) {
            calendar.putAll(data.toMap());
        }
        if (StringUtils.isNotBlank(id)) {
            calendar.setId(id);
        }
        if (StringUtils.isNotBlank(location)) {
            calendar.setLocation(location);
        }
        if (StringUtils.isNotBlank(description)) {
            calendar.setDescription(description);
        }
        if (StringUtils.isNotBlank(timezone)) {
            calendar.setTimeZone(timezone);
        }
        if (deleted) {
            calendar.set("deleted", true);
        }

        return calendar;
    }

    public static GCCalendar fromGoogle(CalendarListEntry googleCalendar) {
        if (googleCalendar == null) {
            return null;
        }
        String summary = googleCalendar.getSummary();
        if (StringUtils.isEmpty(summary)) {
            summary = "-";
            if(googleCalendar.isDeleted()){
                summary = "DELETED";
            }
        }

        GCCalendar calendar = new GCCalendar(summary);
        if(googleCalendar.isDeleted()){
            calendar.setDeleted(true);
        }
        if (StringUtils.isNotBlank(googleCalendar.getDescription())) {
            calendar.setDescription(googleCalendar.getDescription());
        }
        if (StringUtils.isNotBlank(googleCalendar.getLocation())) {
            calendar.setLocation(googleCalendar.getLocation());
        }
        if (StringUtils.isNotBlank(googleCalendar.getTimeZone())) {
            calendar.setTimezone(googleCalendar.getTimeZone());
        }
        if (StringUtils.isNotBlank(googleCalendar.getId())) {
            calendar.setId(googleCalendar.getId());
        }
        calendar.setData(Json.fromMap(googleCalendar));

        return calendar;
    }

    public static GCCalendar fromGoogle(Calendar googleCalendar) {
        if (googleCalendar == null) {
            return null;
        }
        if (StringUtils.isEmpty(googleCalendar.getSummary())) {
            return null;
        }

        GCCalendar calendar = new GCCalendar(googleCalendar.getSummary());
        if (StringUtils.isNotBlank(googleCalendar.getDescription())) {
            calendar.setDescription(googleCalendar.getDescription());
        }
        if (StringUtils.isNotBlank(googleCalendar.getLocation())) {
            calendar.setLocation(googleCalendar.getLocation());
        }
        if (StringUtils.isNotBlank(googleCalendar.getTimeZone())) {
            calendar.setTimezone(googleCalendar.getTimeZone());
        }
        if (StringUtils.isNotBlank(googleCalendar.getId())) {
            calendar.setId(googleCalendar.getId());
        }
        calendar.setData(Json.fromMap(googleCalendar));

        return calendar;
    }

    @Override
    public Json toJson() {
        Json json = Json.map();
        if (data == null) {
            data = Json.map();
        }
        json.set("data", data);

        if (StringUtils.isNotBlank(id)) {
            json.set("calendarId", id);
        } else if (data != null) {
            if(data.contains("id")) {
                json.set("calendarId", data.string("id"));
            } else if(data.contains("calendarId")) {
                json.set("calendarId", data.string("calendarId"));
            }
        }
        if (StringUtils.isNotBlank(summary)) {
            json.set("summary", summary);
        } else if (data != null) {
            json.set("summary", data.string("summary"));
        }
        if (StringUtils.isNotBlank(description)) {
            json.set("description", description);
        } else if (data != null) {
            json.set("description", data.string("description"));
        }
        if (StringUtils.isNotBlank(location)) {
            json.set("location", location);
        } else if (data != null) {
            json.set("location", data.string("location"));
        }
        if (StringUtils.isNotBlank(timezone)) {
            json.set("timezone", timezone);
        } else if (data != null) {
            json.set("timezone", data.string("timeZone"));
            if(StringUtils.isBlank(json.string("timezone"))){
                json.set("timezone", data.string("timezone"));
            }
        }
        if (deleted) {
            json.set("deleted", true);
        }

        return json;
    }

    public static GCCalendar fromJson(Json json) {
        if (json == null) {
            return null;
        }

        GCCalendar calendar = new GCCalendar(json.string("summary"));
        if (StringUtils.isNotBlank(json.string("calendarId"))) {
            calendar.setId(json.string("calendarId"));
        }
        if (StringUtils.isNotBlank(json.string("summary"))) {
            calendar.setSummary(json.string("summary"));
        }
        if (StringUtils.isNotBlank(json.string("description"))) {
            calendar.setDescription(json.string("description"));
        }
        if (StringUtils.isNotBlank(json.string("location"))) {
            calendar.setLocation(json.string("location"));
        }
        if (StringUtils.isNotBlank(json.string("timezone"))) {
            calendar.setTimezone(json.string("timezone"));
        } else if(StringUtils.isNotBlank(json.string("timeZone"))){
            calendar.setTimezone(json.string("timeZone"));
        }
        calendar.setData(json.json("data"));

        return calendar;
    }
}
