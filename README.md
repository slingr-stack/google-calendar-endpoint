---
title: Google Calendar endpoint
keywords: 
last_updated: April 20, 2017
tags: []
summary: "Detailed description of the API of the Google Calendar endpoint."
---

## Overview

The Google Calendar endpoint is a user endpoint (see [Global vs user endpoints](app_development_model_endpoints.html#global-vs-user-endpoints)), 
which means that each user should connect to the endpoint.

This endpoint allows direct access to the [Google Calendar API](https://developers.google.com/google-apps/calendar/v3/reference/),
however it provides shortcuts and helpers for most common use cases.

Some of the features available in this endpoint are:

- Authentication and authorization
- Direct access to the Google Calendar API
- Helpers for most common use cases like create events, find events, list calendars, etc.
- Events when contacts are created, updated or deleted on Google

## Configuration

In order to use the Google Calendar endpoint you must create an app in the [Google Developer Console](https://console.developers.google.com)
by following these instructions:

- Access to Google Developer Console
- Access to `API Manager > Library`. Enable `Calendar API`.
- Access to `API Manager > Credentials > OAuth consent screen`. Complete the form as you prefer and save it.
- Access to `API Manager > Credentials > Credentials`, then `Create credentials > OAuth client ID`.
- Select `Web application` as `Application Type` and add your domain as `Authorized Javascript Origins` (per example 
  `https://myapp.slingrs.io` or you custom domain if you have one), and add a `Authorized redirect URIs` 
  with your domain and `/callback`, like `https://myapp.slingrs.io/callback` or `https://mycustomdomain.com/callback`.
  If you plan to use the app as a template, you should select 'Multi Application' as 'Client type' in order to use the
  platform main domain, like `https://slingrs.io` and `https://slingrs.io/callback`.
- Then click on `Create`.
- That will give you the `Client ID` and `Client Secret` values.  

### Client ID

As explained above, this value comes from the app created in the Google Developer Console.

### Client secret

As explained above, this value comes from the app created in the Google Developer Console.

### Client type

This field determines what kind of URIs will be used on the client configuration on Google.
Select 'Multi Application' when you want to use your application as a template in order to
use the platform main domain.

### Javascript origin

This URL has to be configured in the app created in the Google Developer Console as a valid
origin for OAuth in the field `Authorized JavaScript origins`.

### Registered URI

This URL has to be configured in the app created in the Google Developer Console in the field
`Authorized redirect URIs`.

### Sync process

This setting indicates if the endpoint should periodically check for changes in events, like
for example an event was created or updated. If disabled no events will be received from the
endpoint.

### Sync frequency

How often the endpoint will check for changes in events (in minutes). This value cannot be
less than 5 minutes.

## Quick start

You can create a new event like this:

```js
var calendar = app.endpoints.googleCalendar.findOneCalendar('integrations@slingr.io');
var now = new Date().getTime();
var hour = 1000 * 60 * 60;
var res = app.endpoints.googleCalendar.createEvent(calendar.id, {
  summary: 'test event',
  start: {
    dateTime: app.endpoints.googleCalendar.toGoogleDateTime(now+2*hour)
  },
  end: {
    dateTime: app.endpoints.googleCalendar.toGoogleDateTime(now+3*hour)
  }
});
log('event: '+JSON.stringify(res));
```

Also you get details from an event like this:

```js
var calendar = app.endpoints.googleCalendar.findOneCalendar('integrations@slingr.io');
var event = app.endpoints.googleCalendar.findOneEvent(calendar.id, 'cri5b7bktmm9b5c083vto0v44c');
log('event: '+JSON.stringify(event));
```

If you want to listen for new events or changes on events, you can setup a listener:

```js
var eventInfo = event.data;
var record = sys.data.createRecord('events');
record.field('summary').val(eventInfo.summary);
record.field('start').val(app.endpoints.googleCalendar.fromGoogleDateTime(eventInfo.start.dateTime));
record.field('end').val(app.endpoints.googleCalendar.fromGoogleDateTime(eventInfo.end.dateTime));
sys.data.save(record);
```

## Data formats

We follow the data formats in the Google Calendar API. If there are any difference we will mention
that explicitly.

### Calendars

Documentation for calendars data format can be found [here](https://developers.google.com/google-apps/calendar/v3/reference/calendars).
Here is a sample JSON of a calendar:

```js
{
  "kind": "calendar#calendar",
  "id": "integrations@slingr.io",
  "etag": "\"fBXC91rAg76NkSpaCdEoUEir1ww/TxhV-LOkItwvPkNRgqp9BLQe6cA\"",
  "summary": "integrations@slingr.io",
  "timeZone": "America/Denver"
}
```

### Events

Documentation for events data format can be found [here](https://developers.google.com/google-apps/calendar/v3/reference/events).
Here is a sample JSON of an event:

```js
{
  "kind": "calendar#event",
  "etag": "\"2990110197748000\"",
  "id": "cri5b7bktmm9b5c083vto0v44c",
  "status": "confirmed",
  "htmlLink": "https://www.google.com/calendar/event?eid=Y3JpNWI3Ymt0bW05YjVjMDgzdnRvMHY0NGMgdGVzdC5pbnRlZ3JhdGlvbnNAc2xpbmdyLmlv",
  "created": "2017-05-17T21:04:58.000Z",
  "updated": "2017-05-17T21:04:58.874Z",
  "summary": "test event",
  "description": "test event description",
  "creator": {
    "self": true,
    "email": "integrations@slingr.io"
  },
  "organizer": {
    "self": true,
    "email": "integrations@slingr.io"
  },
  "start": {
    "dateTime": "2017-05-17T17:04:58.000-06:00"
  },
  "end": {
    "dateTime": "2017-05-17T18:04:58.000-06:00"
  },
  "sequence": 0,
  "reminders": {
    "useDefault": true
  },
  "iCalUID": "cri5b7bktmm9b5c083vto0v44c@google.com"
}
```

## Javascript API

The Google Calendar endpoint allows direct access to the API. This means you can make HTTP requests
to access the API documented [here](https://developers.google.com/google-apps/calendar/v3/reference/).

Additionally the endpoint provides shortcuts and helpers for the most common use cases.

### HTTP requests

You can make `GET`, `POST`, `PUT`, and `DELETE` request to the 
[Google Calendar API](https://developers.google.com/google-apps/calendar/v3/reference/) like this:

```js
var calendarList = app.endpoints.googleCalendar.get('users/me/calendarList');
var calendar = app.endpoints.googleCalendar.patch('calendars/'+id, {summary: '3'});
var newCalendar = app.endpoints.googleCalendar.post('calendars', {body: {summary: 'Test calendar'}});
```

Please take a look at the documentation of the [HTTP endpoint]({{site.baseurl}}/endpoints_http.html#javascript-api)
for more information.

### Find one calendar

```js
var calendar = app.endpoints.googleCalendar.findOneCalendar(calendarId);
var calendar = app.endpoints.googleCalendar.findOneCalendar({calendarId: calendarId});
```

Returns the calendar or `null` if not found. Here is a sample:

```js
var calendar = app.endpoints.googleCalendar.findOneCalendar('integrations@slingr.io');
log('calendar: '+JSON.stringify(calendar));
```

### Find calendars

```js
var calendars = app.endpoints.googleCalendar.findCalendars(params);
```

Returns a list of calendars. You will find that list in the field `items` of the response.
Some of the parameters that can be sent are:

- `maxResults`: maximum number of entries returned on one result page. By default the value 
  is 100 entries. The page size can never be larger than 250 entries. Optional.
- `pageToken`: token specifying which result page to return. Optional.
- `showDeleted`: whether to include deleted calendar list entries in the result. Optional.
  The default is `false`.
- `showHidden`: whether to show hidden entries. Optional. The default is `false`.

You can see the full list of parameters [here](https://developers.google.com/google-apps/calendar/v3/reference/calendarList/list).

Here is a sample:

```js
var calendars = app.endpoints.googleCalendar.findCalendars({showDeleted:true, showHidden:true});
calendars.items.forEach(function(calendar) {
  log('calendar ['+calendar.summary+'] has id ['+calendar.id+']');
});
```

### Create calendar

```js
var calendar = app.endpoints.googleCalendar.createCalendar(calendarInfo);
```

Creates a calendar and returns it. Here is a sample:

```js
var calendar = app.endpoints.googleCalendar.createCalendar({summary: "a new calendar"});
log('calendar: '+JSON.stringify(calendar));
```

### Update calendar

```js
var updatedCalendar = app.endpoints.googleCalendar.updateCalendar(calendarId, calendarInfo);
var updatedCalendar = app.endpoints.googleCalendar.updateCalendar({calendarId: calendarId, ...});
```

Updates a calendar and returns it. Here is a sample:

```
var updatedCalendar = app.endpoints.googleCalendar.updateCalendar(calendar.id, {summary: "a new summary"});
log('updated calendar: '+JSON.stringify(updatedCalendar));
```

### Delete calendar

```js
var calendar = app.endpoints.googleCalendar.deleteCalendar(calendarId);
var calendar = app.endpoints.googleCalendar.deleteCalendar({calendarId: calendarId});
```

Deletes a calendar and returns it. Here is a sample:

```js
try {
    var res = app.endpoints.googleCalendar.deleteCalendar('slingr.io_j42sqiqorg41h43qf9n73iquac@group.calendar.google.com');
    log('deleted calendar: '+JSON.stringify(res));
} catch(e) {
    sys.logs.error('calendar was not deleted: '+JSON.stringify(e));
}
```

### Find one event

```js
var event = app.endpoints.googleCalendar.findOneEvent(calendarId, eventId);
var event = app.endpoints.googleCalendar.findOneEvent({calendarId: calendarId, eventId: eventId});
```

Returns the event or `null` if not found. Here is a sample:

```js
var calendar = app.endpoints.googleCalendar.findOneCalendar('integrations@slingr.io');
var event = app.endpoints.googleCalendar.findOneEvent(calendar.id, 'cri5b7bktmm9b5c083vto0v44c');
log('event: '+JSON.stringify(event));
```

### Find events

```js
var events = app.endpoints.googleCalendar.findEvents(calendarId, params);
var events = app.endpoints.googleCalendar.findEvents({calendarId: calendarId, ...});
```

Returns a list of events matching the search criteria. You will find the list of events in the
field `items` of the response. Some of the parameters that can be sent are:

- `q`: free text search terms to find events that match these terms in any field, except for 
  extended properties. Optional.
- `maxResults`: maximum number of events returned on one result page. By default the value is 
  250 events. The page size can never be larger than 2500 events. Optional.
- `orderBy`: the order of the events returned in the result. Optional. The default is an 
  unspecified, stable order. Acceptable values are:
  - `startTime`: order by the start date/time (ascending). This is only available when querying 
    single events (i.e. the parameter `singleEvents` is `true`)
  - `updated`: order by last modification time (ascending).
- `pageToken`: token specifying which result page to return. Optional.
- `showDeleted`: whether to include deleted events (with status equals "cancelled") in the result. 
  Cancelled instances of recurring events (but not the underlying recurring event) will still be 
  included if `showDeleted` and `singleEvents` are both `false`. If `showDeleted` and `singleEvents` 
  are both `true`, only single instances of deleted events (but not the underlying recurring events) 
  are returned. Optional. The default is `false`.
- `singleEvents`: whether to expand recurring events into instances and only return single one-off 
  events and instances of recurring events, but not the underlying recurring events themselves. 
  Optional. The default is `false`.
- `timeMax`: upper bound (exclusive) for an event's start time to filter by. Optional. The default 
  is not to filter by start time. Must be time in milliseconds.
- `timeMin`: lower bound (inclusive) for an event's end time to filter by. Optional. The default 
  is not to filter by end time. Must be time in milliseconds.
- `updatedMin`: lower bound for an event's last modification time (milliseconds timestamp) to filter 
  by. When specified, entries deleted since this time will always be included regardless of 
  `showDeleted`. Optional. The default is not to filter by last modification time.
  
Here is a sample:

```js
var calendar = app.endpoints.googleCalendar.findOneCalendar('integrations@slingr.io');
var events = app.endpoints.googleCalendar.findEvents(calendar.id, {q: 'test', maxResults: 5});
events.items.forEach(function(event) {
  log('event summary ['+event.summary+'] with id ['+event.id+']');
});
```

### Create event

```js
var event = app.endpoints.googleCalendar.createEvent(calendarId, eventInfo);
var event = app.endpoints.googleCalendar.createEvent({calendarId: calendarId, ...});
```

Creates an event in the specified calendar and returns it. Here is a sample:

```js
var calendar = app.endpoints.googleCalendar.findOneCalendar('integrations@slingr.io');
var now = new Date().getTime();
var hour = 1000 * 60 * 60;
var res = app.endpoints.googleCalendar.createEvent(calendar.id, {
  summary: 'test event',
  start: {
    dateTime: app.endpoints.googleCalendar.toGoogleDateTime(now+2*hour)
  },
  end: {
    dateTime: app.endpoints.googleCalendar.toGoogleDateTime(now+3*hour)
  }
});
log('event: '+JSON.stringify(res));
```

### Update event

```js
var event = app.endpoints.googleCalendar.updateEvent(calendarId, eventId, eventInfo);
var event = app.endpoints.googleCalendar.updateEvent({calendarId: calendarId, eventId: eventId, ...});
```

Updates an event an returns it. Here is a sample:

```js
var calendar = app.endpoints.googleCalendar.findOneCalendar('integrations@slingr.io');
var event = app.endpoints.googleCalendar.findOneEvent(calendar.id, 'cri5b7bktmm9b5c083vto0v44c');
var now = new Date().getTime();
var hour = 1000 * 60 * 60;
var eventInfo = {
  summary: 'test event updated',
  start: {
    dateTime: app.endpoints.googleCalendar.toGoogleDateTime(now+2*hour)
  },
  end: {
    dateTime: app.endpoints.googleCalendar.toGoogleDateTime(now+3*hour)
  }
};
var updatedEvent = app.endpoints.googleCalendar.updateEvent(calendar.id, event.id, eventInfo);
log('event updated: '+JSON.stringify(updatedEvent));
```

### Delete event

```js
var event = app.endpoints.googleCalendar.deleteEvent(calendarId, eventId);
var event = app.endpoints.googleCalendar.deleteEvent({calendarId: calendarId, eventId: eventId});
```

Deletes and event and returns it. Here is a sample:

```js
try {
    var calendar = app.endpoints.googleCalendar.findOneCalendar('integrations@slingr.io');
    var event = app.endpoints.googleCalendar.findOneEvent(calendar.id, 'cri5b7bktmm9b5c083vto0v44c');
    var deletedEvent = app.endpoints.googleCalendar.deleteEvent(calendar.id, event.id);
    log('event deleted: '+JSON.stringify(deletedEvent));
} catch(e) {
    sys.logs.error('event was not deleted: '+JSON.stringify(e));
}
```

### Date helpers

The endpoint has some helper methods to make it easy to convert date and date time values from
Google format to SLINGR format and the other way around:

```js
var date = app.endpoints.googleCalendar.fromGoogleDate(googleDate);
var googleDate = app.endpoints.googleCalendar.toGoogleDate(dateObjectOrDateString);
var dateTime = app.endpoints.googleCalendar.fromGoogleDateTime(googleDateTime);
var googleDateTime app.endpoints.googleCalendar.toGoogleDateTime(dateObjectOrMillis);
```

Here is a sample:

```js
var d;

// toGoogleDate
d = new Date();
log('['+app.endpoints.googleCalendar.toGoogleDate(d)+']'); // 2017-04-20
d = new Date().getTime();
log('['+app.endpoints.googleCalendar.toGoogleDate(d)+']'); // 2017-04-20
d = '2017-05-22';
log('['+app.endpoints.googleCalendar.toGoogleDate(d)+']'); // 2017-05-22

// toGoogleDate
d = new Date();
log('['+app.endpoints.googleCalendar.toGoogleDateTime(d)+']'); // 2017-04-20T20:03:11.154Z
d = new Date().getTime();
log('['+app.endpoints.googleCalendar.toGoogleDateTime(d)+']'); // 2017-04-20T20:03:11.321Z

// fromGoogleDateTime
d = '2017-04-20T19:48:19.347Z';
log('['+app.endpoints.googleCalendar.fromGoogleDateTime(d)+']'); // Thu Apr 20 2017 16:48:19 GMT-0300 (ART) <a Date instance>

// fromGoogleDate
d = '2017-04-20';
log('['+app.endpoints.googleCalendar.fromGoogleDate(d)+']'); // 2017-04-20
```

## Events

### Event updated

This event is only sent if the flag `Sync process` is enabled and will indicate that an event
was created or updated. In the `data` field you will find the information about the event:

```js
var eventInfo = event.data;
var record = sys.data.createRecord('events');
record.field('summary').val(eventInfo.summary);
record.field('start').val(app.endpoints.googleCalendar.fromGoogleDateTime(eventInfo.start.dateTime));
record.field('end').val(app.endpoints.googleCalendar.fromGoogleDateTime(eventInfo.end.dateTime));
sys.data.save(record);
```

### Event deleted

This event is only sent if the flag `Sync process` is enabled and will indicate that an event
was deleted. In the `data` field you will find the information about the event:

```js
var event = sys.data.findOne('events', {googleId: event.data.id});
if (event) {
  sys.logs.info('Event ['+event.label()+'] with id ['+event.field('googleId').val()+'] was deleted');
  sys.data.remove(event);
}
```

## About SLINGR

SLINGR is a low-code rapid application development platform that accelerates development, with robust architecture for integrations and executing custom workflows and automation.

[More info about SLINGR](https://slingr.io)

## License

This endpoint is licensed under the Apache License 2.0. See the `LICENSE` file for more details.


