package io.slingr.endpoints.googlecalendar.services.entities;

import com.google.api.services.calendar.model.Calendar;
import io.slingr.endpoints.utils.Json;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Test over the GCCalendar class
 *
 * <p>Created by lefunes on 13/08/15.
 */
public class GCCalendarTest {
    private static final Logger logger = Logger.getLogger(GCCalendarTest.class);

    @Test
    public void testFromJsonConvert() {
        String json = "{" +
                " \"calendarId\":\"ABcdEF123456\" " +
                ", \"summary\":\"A long long long title\" " +
                ", \"description\":\"Here is the description\" " +
                ", \"location\":\"Brasilia, Brazil\" " +
                ", \"timezone\":\"EST\" " +
                ", \"data\": {" +
                "  \"ABC\":\"123\" " +
                ", \"abc\":\"321\" " +
                ", \"DEF\":\"456\" " +
                "} " +
                "}";
        GCCalendar calendar = GCCalendar.fromJson(Json.parse(json));

        Assert.assertNotNull(calendar);
        Assert.assertEquals("ABcdEF123456", calendar.getId());
        Assert.assertEquals("A long long long title", calendar.getSummary());
        Assert.assertEquals("Here is the description", calendar.getDescription());
        Assert.assertEquals("Brasilia, Brazil", calendar.getLocation());
        Assert.assertEquals("EST", calendar.getTimezone());
        Assert.assertNotNull(calendar.getData());
        Assert.assertEquals("123", calendar.getData().string("ABC"));
        Assert.assertEquals("321", calendar.getData().string("abc"));
        Assert.assertEquals("456", calendar.getData().string("DEF"));
        Assert.assertNull(calendar.getData().object("def"));

        json = "{" +
                " \"calendarId\":\"ABcd786\" " +
                ", \"summary\":\"A title\" " +
                "}";
        calendar = GCCalendar.fromJson(Json.parse(json));

        Assert.assertNotNull(calendar);
        Assert.assertEquals("ABcd786", calendar.getId());
        Assert.assertEquals("A title", calendar.getSummary());
        Assert.assertNull(calendar.getDescription());
        Assert.assertNull(calendar.getLocation());
        Assert.assertNull(calendar.getTimezone());
        Assert.assertNotNull(calendar.getData());
        Assert.assertNull(calendar.getData().object("ABC"));

        logger.info("-- END");
    }

    @Test
    public void testFromCalendarConvert() {
        String json = "{" +
                " \"calendarId\":\"ABcdEF123456\" " +
                ", \"summary\":\"A long long long title\" " +
                ", \"description\":\"Here is the description\" " +
                ", \"location\":\"Brasilia, Brazil\" " +
                ", \"timezone\":\"EST\" " +
                ", \"data\": {" +
                "  \"ABC\":\"123\" " +
                ", \"abc\":\"321\" " +
                ", \"DEF\":\"456\" " +
                "} " +
                "}";
        GCCalendar calendar = GCCalendar.fromJson(Json.parse(json));
        Json result = calendar.toJson();

        Assert.assertNotNull(result);
        Assert.assertEquals("ABcdEF123456", result.string("calendarId"));
        Assert.assertEquals("A long long long title", result.string("summary"));
        Assert.assertEquals("Here is the description", result.string("description"));
        Assert.assertEquals("Brasilia, Brazil", result.string("location"));
        Assert.assertEquals("EST", result.string("timezone"));
        Assert.assertNotNull(result.object("data"));
        Assert.assertEquals("123", result.json("data").string("ABC"));
        Assert.assertEquals("321", result.json("data").string("abc"));
        Assert.assertEquals("456", result.json("data").string("DEF"));
        Assert.assertNull(result.json("data").string("fff"));

        logger.info("-- END");
    }

    @Test
    public void testFromGoogleCalendarConvert() {
        String json = "{" +
                " \"calendarId\":\"ABcdEF123456\" " +
                ", \"summary\":\"A long long long title\" " +
                ", \"description\":\"Here is the description\" " +
                ", \"location\":\"Brasilia, Brazil\" " +
                ", \"timezone\":\"EST\" " +
                ", \"data\": {" +
                "  \"ABC\":\"123\" " +
                ", \"abc\":\"321\" " +
                ", \"DEF\":\"456\" " +
                "} " +
                "}";
        GCCalendar calendar = GCCalendar.fromJson(Json.parse(json));
        Calendar gCalendar = calendar.toGoogle();

        Assert.assertNotNull(gCalendar);
        Assert.assertEquals("ABcdEF123456", gCalendar.getId());
        Assert.assertEquals("A long long long title", gCalendar.getSummary());
        Assert.assertEquals("Here is the description", gCalendar.getDescription());
        Assert.assertEquals("Brasilia, Brazil", gCalendar.getLocation());
        Assert.assertEquals("EST", gCalendar.getTimeZone());
        Assert.assertEquals("123", gCalendar.get("ABC"));
        Assert.assertEquals("321", gCalendar.get("abc"));
        Assert.assertEquals("456", gCalendar.get("DEF"));
        Assert.assertNull(gCalendar.get("fff"));

        GCCalendar calendar2 = GCCalendar.fromGoogle(gCalendar);

        Assert.assertNotNull(calendar2);
        Assert.assertEquals("ABcdEF123456", calendar2.getId());
        Assert.assertEquals("A long long long title", calendar2.getSummary());
        Assert.assertEquals("Here is the description", calendar2.getDescription());
        Assert.assertEquals("Brasilia, Brazil", calendar2.getLocation());
        Assert.assertEquals("EST", calendar2.getTimezone());
        Assert.assertNotNull(calendar2.getData());
        Assert.assertEquals("123", calendar2.getData().string("ABC"));
        Assert.assertEquals("321", calendar2.getData().string("abc"));
        Assert.assertEquals("456", calendar2.getData().string("DEF"));
        Assert.assertNull(calendar2.getData().string("def"));

        logger.info("-- END");
    }
}