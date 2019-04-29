/////////////////////
// Public API
/////////////////////

endpoint.findOneCalendar = function (calendarId, options) {
    options = checkOptions(calendarId, options, 'calendarId');
    checkValue(options, 'calendarId');
    var response = endpoint._findOneCalendar(options);
    if (!response || isEmptyMap(response)) {
        return null;
    }
    return response;
};

endpoint.findCalendars = function (params) {
    params = checkOptions(null, params);
    return endpoint._findCalendars(params);
};

endpoint.createCalendar = function (calendar) {
    calendar = checkOptions(null, calendar);
    return endpoint._createCalendar(calendar);
};

endpoint.updateCalendar = function (calendarId, calendar) {
    calendar = checkOptions(calendarId, calendar, 'calendarId');
    checkValue(calendar, 'calendarId');
    return endpoint._updateCalendar(calendar);
};

endpoint.deleteCalendar = function (calendarId, options) {
    options = checkOptions(calendarId, options, 'calendarId');
    checkValue(options, 'calendarId');
    var response = endpoint._deleteCalendar(options);
    if (!!response && !!response.__endpoint_exception__) {
        throw response;
    }
    return response;
};

endpoint.findOneEvent = function (calendarId, eventId, options) {
    options = checkOptions(eventId, options, 'eventId');
    options = checkOptions(calendarId, options, 'calendarId');
    checkValue(options, 'calendarId');
    checkValue(options, 'eventId');
    var response = endpoint._findOneEvent(options);
    if (!response || isEmptyMap(response)) {
        return null;
    }
    return response;
};

endpoint.findEvents = function (calendarId, params) {
    params = checkOptions(calendarId, params, 'calendarId');
    checkValue(params, 'calendarId');
    return endpoint._findEvents(params);
};

endpoint.createEvent = function (calendarId, event) {
    event = checkOptions(calendarId, event, 'calendarId');
    checkValue(event, 'calendarId');
    return endpoint._createEvent(event);
};

endpoint.updateEvent = function (calendarId, eventId, event) {
    event = checkOptions(eventId, event, 'eventId');
    event = checkOptions(calendarId, event, 'calendarId');
    checkValue(event, 'calendarId');
    checkValue(event, 'eventId');
    return endpoint._updateEvent(event);
};

endpoint.deleteEvent = function (calendarId, eventId, options) {
    options = checkOptions(eventId, options, 'eventId');
    options = checkOptions(calendarId, options, 'calendarId');
    checkValue(options, 'calendarId');
    checkValue(options, 'eventId');
    var response = endpoint._deleteEvent(options);
    if (!!response && !!response.__endpoint_exception__) {
        throw response;
    }
    return response;
};

/////////////////////
// Public API - Generic Functions
/////////////////////

endpoint.get = function (url) {
    options = checkHttpOptions(url, {});
    return endpoint._getRequest(options);
};

endpoint.post = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._postRequest(options);
};

endpoint.put = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._putRequest(options);
};

endpoint.patch = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._patchRequest(options);
};

endpoint.delete = function (url) {
    var options = checkHttpOptions(url, {});
    return endpoint._deleteRequest(options);
};

/////////////////////
// Old Public API
/////////////////////
endpoint.old = endpoint.old || {};

endpoint.old.getCalendars = function (options) {
    return executeOldFunction('getCalendars', options)
};

endpoint.old.createCalendar = function (options) {
    return executeOldFunction('createCalendar', options)
};

endpoint.old.updateCalendar = function (options) {
    return executeOldFunction('updateCalendar', options)
};

endpoint.old.removeCalendar = function (options) {
    return executeOldFunction('removeCalendar', options)
};

endpoint.old.clearCalendar = function (options) {
    return executeOldFunction('clearCalendar', options)
};

endpoint.old.getEvents = function (options) {
    return executeOldFunction('getEvents', options)
};

endpoint.old.syncEvents = function (options) {
    return executeOldFunction('syncEvents', options)
};

endpoint.old.createEvent = function (options) {
    return executeOldFunction('createEvent', options)
};

endpoint.old.updateEvent = function (options) {
    return executeOldFunction('updateEvent', options)
};

endpoint.old.removeEvent = function (options) {
    return executeOldFunction('removeEvent', options)
};

endpoint.old.convertEvent = function (event) {
    var oldEvent = {};
    if (event) {
        oldEvent.eventId = event.id;
        oldEvent.calendarId = event.calendarId;  // TODO add calendar id!
        oldEvent.data = event;
        oldEvent.cancelled = event.status === "cancelled";
        oldEvent.summary = event.summary;
        oldEvent.allDayEvent = false;
        if (!!event.start) {
            if (!!event.start.date) {
                oldEvent.allDayEvent = true;
                oldEvent.startDate = event.start.date;
                oldEvent.start = oldEvent.startDate + 'T00:00:00.000Z';
            } else if (!!event.start.dateTime) {
                oldEvent.start = event.start.dateTime;
                oldEvent.startDate = oldEvent.start.substring(0, 10);
            }
            if (!!event.start.timeZone) {
                oldEvent.timezone = event.start.timeZone;
            }
        }
        if (!!event.end) {
            if (!!event.end.date) {
                oldEvent.allDayEvent = true;
                oldEvent.endDate = event.end.date;
                oldEvent.end = oldEvent.endDate + 'T00:00:00.000Z';
            } else if (!!event.end.dateTime) {
                oldEvent.end = event.end.dateTime;
                oldEvent.endDate = oldEvent.end.substring(0, 10);
            }
            if (!!event.end.timeZone) {
                oldEvent.timezone = event.end.timeZone;
            }
        }
    }
    return oldEvent;
};

/////////////////////
// Public API - Conversions
/////////////////////

endpoint.toGoogleDateTime = function (value) {
    return new Date(value).toJSON();
};

endpoint.toGoogleDate = function (value) {
    return endpoint.toGoogleDateTime(value).substring(0, 10);
};

endpoint.fromGoogleDateTime = function (value) {
    return new Date(value);
};

endpoint.fromGoogleDate = function (value) {
    return endpoint.toGoogleDateTime(value).substring(0, 10);
};

/////////////////////
// Utilities
/////////////////////

var executeOldFunction = function (fcName, options) {
    options = options || {};
    options.__functionName = fcName;

    var response;
    try {
        response = endpoint._oldFunction(options);
    } catch (err) {
        // replace the name of the endpoint function by the name of the js function
        throw convertExceptionAndReplace(err, '_oldFunction', 'old.' + fcName);
    }
    return response;
};

var checkHttpOptions = function (url, options) {
    options = options || {};
    if (!!url) {
        if (isObject(url)) {
            // take the 'url' parameter as the options
            options = url || {};
        } else {
            if (!!options.path || !!options.params || !!options.body) {
                // options contains the http package format
                options.path = url;
            } else {
                // create html package
                options = {
                    path: url,
                    body: options
                }
            }
        }
    }
    return options;
};

var checkOptions = function (id, options, idKey) {
    options = options || {};
    if (!!id) {
        if (isObject(id)) {
            // take the 'id' parameter as the options
            options = id || {};
        } else if (!!idKey && !!id) {
            // replace the id on 'options' by the received on parameters
            options[idKey] = id;
        }
    }
    return options;
};

var isObject = function (obj) {
    return !!obj && stringType(obj) === '[object Object]'
};

var stringType = Function.prototype.call.bind(Object.prototype.toString);

var checkValue = function (options, idKey) {
    if (!options[idKey]) {
        // exception if value is not present
        throw 'Empty ' + idKey;
    }
};

var isEmptyMap = function(obj) {
    for(var prop in obj) {
        if(obj.hasOwnProperty(prop)) {
            return false;
        }
    }

    return JSON.stringify(obj) === JSON.stringify({});
};
