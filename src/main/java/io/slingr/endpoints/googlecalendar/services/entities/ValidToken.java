package io.slingr.endpoints.googlecalendar.services.entities;

import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import io.slingr.endpoints.googlecalendar.services.GoogleCalendarService;
import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang3.StringUtils;

import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Valid token to perform request to Google
 * 
 * Created by lefunes on 21/10/14.
 */
public class ValidToken {
    
    public static final String TOKEN = "token";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String EXPIRATION_TIME = "expirationTime";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(GoogleCalendarService.EXPIRATION_TIME_FORMAT);
    private static final DateFormat DATE_FORMAT_2 =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    private String token = null;
	private final String refreshToken;
	private Date expirationTime = null;

    public ValidToken(String token, String refreshToken, String expirationTime) {
        this.token = token;
        this.refreshToken = refreshToken;
        if(StringUtils.isNotBlank(expirationTime)) {
            try {
                this.expirationTime = DATE_FORMAT.parse(expirationTime);
            } catch (ParseException e) {
                try {
                    this.expirationTime = DATE_FORMAT_2.parse(expirationTime);
                } catch (ParseException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

	public ValidToken(GoogleTokenResponse response) {
		if (response == null) {
			throw new InvalidParameterException("Google token must be a not null object.");
		}
        this.refreshToken = response.getRefreshToken();
        updateToken(response);
	}

	public String getToken() {
		return token;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public Date getExpirationTime() {
		return expirationTime;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setExpirationTime(Date expirationTime) {
		this.expirationTime = expirationTime;
	}

	public void updateToken(GoogleTokenResponse response) {
        this.token = response.getAccessToken();

        Calendar time = Calendar.getInstance();
        if(response.getExpiresInSeconds() != null) {
            time.add(Calendar.SECOND, response.getExpiresInSeconds().intValue());
        } else {
            time.add(Calendar.DATE, 30);
        }
        this.expirationTime = time.getTime();
	}

    public boolean isExpired() {
        return StringUtils.isBlank(token) || (expirationTime != null && (System.currentTimeMillis() - expirationTime.getTime() > 0));
    }

    public Json toJson() {
        final Json json = Json.map();
        json.set(TOKEN, token);
        json.set(REFRESH_TOKEN, refreshToken);
        if(expirationTime != null) {
            json.set(EXPIRATION_TIME, DATE_FORMAT.format(expirationTime));
        }
        return json;
    }
}
