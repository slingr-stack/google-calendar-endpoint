package io.slingr.endpoints.googlecalendar.services.utils;

import com.google.api.client.util.DateTime;

import java.util.TimeZone;

/**
 * <p>Methods that permit to work with the DateTime class
 *
 * <p>Created by lefunes on 12/08/15.
 */
public class DateTimeUtils {

    public static DateTime getDateTime(Object dt, String timezone){
        TimeZone tz = timezone != null ? TimeZone.getTimeZone(timezone) : null;
        if(dt instanceof DateTime) {
            return (DateTime) dt;
        } else if(dt instanceof Number) {
            return new DateTime(((Number)dt).longValue(), tz != null ? tz.getRawOffset()/1000/60 : 0);
        } else if(dt != null) {
            return new DateTime(dt.toString());
        }
        return null;
    }

    public static String getOnlyDate(String dt){
        if(dt == null){
            return null;
        }
        int p = dt.indexOf("T");
        if(p < 0){
            return dt;
        } else {
            return dt.substring(0, p);
        }
    }

}
