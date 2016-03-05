package com.henry.iwagenda;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Offline {
    private Context context;
    private final UserResources ur;

    Offline(Context context) {
        this.context = context;
        this.ur = new UserResources(context);
    }

    private final iwAPI iw = new iwAPI();

    /**
     * Starts sync service if offline data is at least one week old
     */
    protected void syncIfNeeded() {
        Date lastUpdateDateMax = getLastUpdateDate();
        if (lastUpdateDateMax != null) {
            Date today = new Date();
            // Add 7 days to lastUpdateDateMax
            Calendar c = Calendar.getInstance();
            c.setTime(lastUpdateDateMax);
            c.add(Calendar.DATE, 7);
            lastUpdateDateMax.setTime( c.getTime().getTime() );

            if (today.after(lastUpdateDateMax)) {
                // syncOffline(agendas, cookies, months);
                startSyncService();
            }
        }
    }

    protected void startSyncService() {
        Intent startIntent = new Intent(context, iwSyncService.class);
        startIntent.setAction("com.henry.iwagenda.action.start_sync");
        context.startService(startIntent);
    }

    /**
    * Syncs latest data from server
    *
    * @param  cookies  Session cookies (obtained after login)
    */
    protected void syncOffline(Map<String,String> cookies) {
        Set<Event> events = new HashSet<>();
        Calendar present = Calendar.getInstance();

        Calendar lastDate = Calendar.getInstance();
        lastDate.add(Calendar.MONTH, ur.getSyncMonths());

        try {
            for (present.get(Calendar.MONTH); present.before(lastDate); present.add(Calendar.MONTH, 1)) {
                for (Agenda a : ur.getUserSelectedAgendas()) {
                    for (Event e : iw.getEventsForMonth(a, present.getTime(), cookies)) {
                        events.add(e);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (!events.isEmpty()) {
            Gson gson = new Gson();
            String eventsJSON = gson.toJson(events);
            SharedPreferences offline = context.getSharedPreferences("offline", Context.MODE_PRIVATE);
            SharedPreferences.Editor offlineEdit = offline.edit();
            offlineEdit.putString("offlineEvents", eventsJSON);

            Date lastUpdate = new Date();
            String lastUpdateJSON = gson.toJson(lastUpdate);
            offlineEdit.putString("lastSync", lastUpdateJSON);
            offlineEdit.commit();
        }
    }

    private Date getLastUpdateDate() {
        SharedPreferences offline = context.getSharedPreferences("offline", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = offline.getString("lastSync", null);
        Type type = new TypeToken<Date>(){}.getType();
        return gson.fromJson(json, type);
    }

    /**
     * Get offline events for a date
     *
     * @param  date     Date to get the events from
     * @return          Events for date (Set)
     */
    protected Set<Event> getOfflineEventsForDate(Date date) {
        Set<Event> offlineEvents;
        Set<Event> events = new HashSet<>();
        SharedPreferences offline = context.getSharedPreferences("offline", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = offline.getString("offlineEvents", null);
        Type type = new TypeToken<Set<Event>>(){}.getType();
        offlineEvents = gson.fromJson(json, type);
        // Retrieve all the events
        if (offlineEvents != null) {
            for (Event e : offlineEvents) {
                if (e.getDate().equals(date)) {
                    events.add(e);
                }
            }
        }
        return events;
    }

    protected Set<Date> getOfflineEventsDates() {
        Set<Date> eventDates = new HashSet<>();
        Set<Event> offlineEvents;
        SharedPreferences offline = context.getSharedPreferences("offline", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = offline.getString("offlineEvents", null);
        Type type = new TypeToken<Set<Event>>(){}.getType();
        offlineEvents = gson.fromJson(json, type);
        // Retrieve all the events
        if (offlineEvents != null) {
            for (Event e : offlineEvents) {
                if (!eventDates.contains(e.getDate()))
                    eventDates.add(e.getDate());
            }
        }
        return eventDates;
    }
}
