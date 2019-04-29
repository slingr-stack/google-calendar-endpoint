package io.slingr.endpoints.googlecalendar.services.entities;

import com.google.api.services.calendar.model.Event;
import io.slingr.endpoints.utils.Json;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Tests over the GCEvent class
 *
 * <p>Created by lefunes on 13/08/15.
 */
public class GCEventTest {
    private static final Logger logger = Logger.getLogger(GCEventTest.class);

    @Test
    public void testFromJsonConvert() {
        String json = "{" +
                "\"start\":\"1995-04-12T10:10:10.10Z\" " +
                ", \"end\":\"2005-04-12T23:20:50.52Z\" " +
                ", \"allDayEvent\":\"false\" " +
                ", \"timezone\":\"PST\" " +
                ", \"location\":\"Barcelona, Spain\" " +
                ", \"description\":\"Here is the description\" " +
                ", \"summary\":\"A long long long title\" " +
                ", \"eventId\":\"ABcdEF123456\" " +
                ", \"data\": {" +
                "  \"ABC\":\"123\" " +
                ", \"abc\":\"321\" " +
                ", \"DEF\":\"123\" " +
                "} " +
                "}";
        GCEvent event = GCEvent.fromJson("123456", Json.parse(json));

        Assert.assertNotNull(event);
        Assert.assertEquals("123456", event.getCalendarId());
        Assert.assertEquals("ABcdEF123456", event.getId());
        Assert.assertEquals("A long long long title", event.getSummary());
        Assert.assertEquals("Here is the description", event.getDescription());
        Assert.assertEquals("Barcelona, Spain", event.getLocation());
        Assert.assertEquals("1995-04-12T10:10:10.010Z", event.getStart());
        Assert.assertEquals("2005-04-12T23:20:50.052Z", event.getEnd());
        Assert.assertEquals(false, event.isAllDayEvent());
        Assert.assertNotNull(event.getData());
        Assert.assertEquals("123", event.getData().string("ABC"));
        Assert.assertEquals("321", event.getData().string("abc"));
        Assert.assertEquals("123", event.getData().string("DEF"));
        Assert.assertNull(event.getData().object("def"));

        json = "{" +
                "\"start\":\"1995-04-12T10:10:10.10Z\" " +
                ", \"end\":\"2005-04-12T23:20:50.52Z\" " +
                ", \"allDayEvent\":\"true\" " +
                ", \"timezone\":\"PST\" " +
                ", \"location\":\"Barcelona, Spain\" " +
                ", \"description\":\"Here is the description\" " +
                ", \"summary\":\"A long long long title\" " +
                ", \"eventId\":\"ABcdEF123456\" " +
                ", \"data\": {" +
                "  \"ABC\":\"123\" " +
                ", \"abc\":\"321\" " +
                ", \"DEF\":\"123\" " +
                "} " +
                "}";
        event = GCEvent.fromJson("123456", Json.parse(json));

        Assert.assertNotNull(event);
        Assert.assertEquals("123456", event.getCalendarId());
        Assert.assertEquals("ABcdEF123456", event.getId());
        Assert.assertEquals("A long long long title", event.getSummary());
        Assert.assertEquals("Here is the description", event.getDescription());
        Assert.assertEquals("Barcelona, Spain", event.getLocation());
        Assert.assertEquals("1995-04-12T10:10:10.010Z", event.getStart());
        Assert.assertEquals("2005-04-12T23:20:50.052Z", event.getEnd());
        Assert.assertEquals(true, event.isAllDayEvent());
        Assert.assertNotNull(event.getData());
        Assert.assertEquals("123", event.getData().string("ABC"));
        Assert.assertEquals("321", event.getData().string("abc"));
        Assert.assertEquals("123", event.getData().string("DEF"));
        Assert.assertNull(event.getData().object("def"));

        logger.info("-- END");
    }

    @Test
    public void testFromEventConvert() {
        String json = "{" +
                "\"start\":\"1995-04-12T10:10:10.10Z\" " +
                ", \"end\":\"2005-04-12T23:20:50.52Z\" " +
                ", \"allDayEvent\":\"false\" " +
                ", \"timezone\":\"PST\" " +
                ", \"location\":\"Barcelona, Spain\" " +
                ", \"description\":\"Here is the description\" " +
                ", \"summary\":\"A long long long title\" " +
                ", \"eventId\":\"ABcdEF123456\" " +
                ", \"data\": {" +
                "  \"ABC\":\"123\" " +
                ", \"abc\":\"321\" " +
                ", \"DEF\":\"123\" " +
                "} " +
                "}";
        GCEvent event = GCEvent.fromJson("123456", Json.parse(json));
        Assert.assertNotNull(event);
        Json result = event.toJson();

        Assert.assertNotNull(result);
        Assert.assertEquals("123456", result.string("calendarId"));
        Assert.assertEquals("ABcdEF123456", result.string("eventId"));
        Assert.assertEquals("A long long long title", result.string("summary"));
        Assert.assertEquals("Here is the description", result.string("description"));
        Assert.assertEquals("Barcelona, Spain", result.string("location"));
        Assert.assertEquals("1995-04-12T10:10:10.010Z",result.string("start"));
        Assert.assertEquals("2005-04-12T23:20:50.052Z", result.string("end"));
        Assert.assertEquals("1995-04-12",result.string("startDate"));
        Assert.assertEquals("2005-04-12", result.string("endDate"));
        Assert.assertEquals(Boolean.FALSE, result.bool("allDayEvent"));
        Assert.assertNotNull(result.object("data"));
        Assert.assertEquals("123", result.json("data").string("ABC"));
        Assert.assertEquals("321", result.json("data").string("abc"));
        Assert.assertEquals("123", result.json("data").string("DEF"));
        Assert.assertNull(result.json("data").string("fff"));

        logger.info("-- END");
    }

    @Test
    public void testFromGoogleEventConvert() {
        String json = "{" +
                "\"start\":\"1995-04-12T10:10:10.10Z\" " +
                ", \"end\":\"2005-04-12T23:20:50.52Z\" " +
                ", \"allDayEvent\":\"false\" " +
                ", \"timezone\":\"PST\" " +
                ", \"location\":\"Barcelona, Spain\" " +
                ", \"description\":\"Here is the description\" " +
                ", \"summary\":\"A long long long title\" " +
                ", \"eventId\":\"ABcdEF123456\" " +
                ", \"data\": {" +
                "  \"ABC\":\"123\" " +
                ", \"abc\":\"321\" " +
                ", \"DEF\":\"123\" " +
                "} " +
                "}";
        GCEvent event = GCEvent.fromJson("123456", Json.parse(json));
        Assert.assertNotNull(event);
        com.google.api.services.calendar.model.Event gEvent = event.toGoogle();

        Assert.assertNotNull(gEvent);
        Assert.assertEquals("ABcdEF123456", gEvent.getId());
        Assert.assertEquals("A long long long title", gEvent.getSummary());
        Assert.assertEquals("Here is the description", gEvent.getDescription());
        Assert.assertEquals("Barcelona, Spain", gEvent.getLocation());
        Assert.assertNotNull(gEvent.getStart());
        Assert.assertNotNull(gEvent.getStart().get("dateTime"));
        Assert.assertEquals("1995-04-12T10:10:10.010Z", gEvent.getStart().get("dateTime").toString());
        Assert.assertNotNull(gEvent.getEnd());
        Assert.assertNotNull(gEvent.getEnd().get("dateTime"));
        Assert.assertEquals("2005-04-12T23:20:50.052Z", gEvent.getEnd().get("dateTime").toString());
        Assert.assertEquals("123", gEvent.get("ABC"));
        Assert.assertEquals("321", gEvent.get("abc"));
        Assert.assertEquals("123", gEvent.get("DEF"));
        Assert.assertNull(gEvent.get("fff"));

        GCEvent event2 = GCEvent.fromGoogle("123456", gEvent);

        Assert.assertNotNull(event2);
        Assert.assertEquals("123456", event2.getCalendarId());
        Assert.assertEquals("ABcdEF123456", event2.getId());
        Assert.assertEquals("A long long long title", event2.getSummary());
        Assert.assertEquals("Here is the description", event2.getDescription());
        Assert.assertEquals("Barcelona, Spain", event2.getLocation());
        Assert.assertEquals("1995-04-12T10:10:10.010Z", event2.getStart());
        Assert.assertEquals("2005-04-12T23:20:50.052Z", event2.getEnd());
        Assert.assertEquals(false, event2.isAllDayEvent());
        Assert.assertNotNull(event2.getData());
        Assert.assertEquals("123", event2.getData().string("ABC"));
        Assert.assertEquals("321", event2.getData().string("abc"));
        Assert.assertEquals("123", event2.getData().string("DEF"));
        Assert.assertNull(event2.getData().object("def"));

        Assert.assertEquals(false, event2.isCancelled());

        logger.info("-- END");
    }

    @Test
    public void testFromGoogleEventConvertAllDate() {
        String json = "{" +
                "\"start\":\"1995-04-12T10:10:10.10Z\" " +
                ", \"end\":\"2005-04-13T23:20:50.52Z\" " +
                ", \"allDayEvent\":\"true\" " +
                ", \"timezone\":\"PST\" " +
                ", \"location\":\"Barcelona, Spain\" " +
                ", \"description\":\"Here is the description\" " +
                ", \"summary\":\"A long long long title\" " +
                ", \"eventId\":\"ABcdEF123456\" " +
                ", \"data\": {" +
                "  \"ABC\":\"123\" " +
                ", \"abc\":\"321\" " +
                ", \"DEF\":\"123\" " +
                "} " +
                "}";
        GCEvent event = GCEvent.fromJson("123456", Json.parse(json));
        Assert.assertNotNull(event);
        com.google.api.services.calendar.model.Event gEvent = event.toGoogle();

        Assert.assertNotNull(gEvent);
        Assert.assertEquals("ABcdEF123456", gEvent.getId());
        Assert.assertEquals("A long long long title", gEvent.getSummary());
        Assert.assertEquals("Here is the description", gEvent.getDescription());
        Assert.assertEquals("Barcelona, Spain", gEvent.getLocation());
        Assert.assertNotNull(gEvent.getStart());
        Assert.assertNotNull(gEvent.getStart().get("date"));
        Assert.assertEquals("1995-04-12", gEvent.getStart().get("date").toString());
        Assert.assertNotNull(gEvent.getEnd());
        Assert.assertNotNull(gEvent.getEnd().get("date"));
        Assert.assertEquals("2005-04-13", gEvent.getEnd().get("date").toString());
        Assert.assertEquals("123", gEvent.get("ABC"));
        Assert.assertEquals("321", gEvent.get("abc"));
        Assert.assertEquals("123", gEvent.get("DEF"));
        Assert.assertNull(gEvent.get("fff"));

        GCEvent event2 = GCEvent.fromGoogle("123456", gEvent);

        Assert.assertNotNull(event2);
        Assert.assertEquals("123456", event2.getCalendarId());
        Assert.assertEquals("ABcdEF123456", event2.getId());
        Assert.assertEquals("A long long long title", event2.getSummary());
        Assert.assertEquals("Here is the description", event2.getDescription());
        Assert.assertEquals("Barcelona, Spain", event2.getLocation());
        Assert.assertEquals("1995-04-12", event2.getStart());
        Assert.assertEquals("2005-04-13", event2.getEnd());
        Assert.assertEquals(true, event2.isAllDayEvent());
        Assert.assertNotNull(event2.getData());
        Assert.assertEquals("123", event2.getData().string("ABC"));
        Assert.assertEquals("321", event2.getData().string("abc"));
        Assert.assertEquals("123", event2.getData().string("DEF"));
        Assert.assertNull(event2.getData().object("def"));

        Assert.assertEquals(false, event2.isCancelled());

        logger.info("-- END");
    }

    @Test
    public void testFromGoogleEventConvertCancelled() {
        String json = "{" +
                "\"start\":\"1995-04-12T10:10:10.10Z\" " +
                ", \"end\":\"2005-04-12T23:20:50.52Z\" " +
                ", \"allDayEvent\":\"false\" " +
                ", \"timezone\":\"PST\" " +
                ", \"location\":\"Barcelona, Spain\" " +
                ", \"description\":\"Here is the description\" " +
                ", \"summary\":\"A long long long title\" " +
                ", \"eventId\":\"ABcdEF123456\" " +
                ", \"status\":\"CANceLLEd\" " +
                ", \"data\": {" +
                "  \"ABC\":\"123\" " +
                ", \"abc\":\"321\" " +
                ", \"DEF\":\"123\" " +
                "} " +
                "}";
        GCEvent event = GCEvent.fromJson("123456", Json.parse(json));
        Assert.assertNotNull(event);
        Event gEvent = event.toGoogle();

        Assert.assertNotNull(gEvent);
        Assert.assertEquals("ABcdEF123456", gEvent.getId());
        Assert.assertEquals("cancelled", gEvent.getStatus());

        GCEvent event2 = GCEvent.fromGoogle("123456", gEvent);

        Assert.assertNotNull(event2);
        Assert.assertEquals("123456", event2.getCalendarId());
        Assert.assertEquals(true, event2.isCancelled());

        logger.info("-- END");

    }

    @Test
    public void testFromGoogleEventConvertCancelled2() {
        String json = "{" +
                " \"eventId\":\"ABcdEF123456\" " +
                ", \"status\":\"canceLLEd\" " +
                "}";
        GCEvent event = GCEvent.fromJson("123456", Json.parse(json));
        Assert.assertNotNull(event);
        com.google.api.services.calendar.model.Event gEvent = event.toGoogle();

        Assert.assertNotNull(gEvent);
        Assert.assertEquals("ABcdEF123456", gEvent.getId());
        Assert.assertEquals("cancelled", gEvent.getStatus());

        GCEvent event2 = GCEvent.fromGoogle("123456", gEvent);

        Assert.assertNotNull(event2);
        Assert.assertEquals("123456", event2.getCalendarId());
        Assert.assertEquals("ABcdEF123456", event2.getId());
        Assert.assertEquals(true, event2.isCancelled());

        logger.info("-- END");
    }
}