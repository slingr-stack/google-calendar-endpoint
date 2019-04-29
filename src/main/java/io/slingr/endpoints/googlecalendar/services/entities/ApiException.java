package io.slingr.endpoints.googlecalendar.services.entities;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpResponseException;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.exceptions.PermanentException;
import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang3.StringUtils;

/**
 * Exception helper
 *
 * <p>Created by lefunes on 21/10/14.
 */
public class ApiException {

    public static Json generate(Exception ex, boolean includeFlag){
       return generate(ex).toJson(includeFlag);
    }

    public static PermanentException generate(Exception ex){
        return generate(null, ex);
    }

    public static Json generate(String contextMessage, Exception ex, boolean includeFlag){
        return generate(contextMessage, ex).toJson(includeFlag);
    }

    public static PermanentException generate(String contextMessage, Exception ex){
        Json additionalInfo = Json.map();
        String description = "-";
        String code = "-";
        if(ex != null) {
            if (ex instanceof PermanentException) {
               return (PermanentException) ex;
            } else if (ex instanceof EndpointException) {
               return EndpointException.permanent(((EndpointException) ex).getCode(), ex.getMessage(), ((EndpointException) ex).getAdditionalInfo());
            } else if (ex instanceof GoogleJsonResponseException) {
                description = ((GoogleJsonResponseException) ex).getDetails().getMessage();
                code = ""+((GoogleJsonResponseException) ex).getDetails().getCode();
                additionalInfo.set("googleException", Json.fromMap(((GoogleJsonResponseException) ex).getDetails()));
            } else if (ex instanceof HttpResponseException) {
                description = ((HttpResponseException) ex).getContent() != null ? ((HttpResponseException) ex).getContent() : ex.getMessage();
                code = ((HttpResponseException) ex).getStatusMessage();
            } else {
                description = ex.getMessage();
            }
        }

        contextMessage = StringUtils.isNotBlank(contextMessage) ? contextMessage : "Google API Exception";
        String message = String.format("%s [%s], code [%s]", contextMessage, description, code);
        if(contextMessage.contains(description)){
            message = String.format("%s, code [%s]", contextMessage, code);
        }
        return EndpointException.permanent(ErrorCode.API, message, additionalInfo, ex);
    }
}
