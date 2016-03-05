package com.henry.iwagenda;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserResources {
    private iwAPI iw = new iwAPI();
    private Context context;

    UserResources(Context context) {
        this.context = context;
    }

    /**
     * Gets the agendas the user can write to
     *
     * @param  cookies  Session cookies (obtained after login)
     * @return          Writable agendas (Set)
     */
    protected Set<Agenda> getWritableAgendasForUser(Map<String, String> cookies) {
        Set<Agenda> writableAgendasForUser = new HashSet<>();
        Gson gson = new Gson();
        SharedPreferences agendas = context.getSharedPreferences("agendas", Context.MODE_PRIVATE);
        String writableAgendasJSON = agendas.getString("writableAgendas", null);

        if (writableAgendasJSON != null) {
            Type type = new TypeToken<Set<Agenda>>(){}.getType();
            writableAgendasForUser = gson.fromJson(writableAgendasJSON, type);
        }
        if (writableAgendasForUser.isEmpty()) {
            Set<Agenda> agendasAvailableForUser = getUserAgendas(cookies);
            for (Agenda a : agendasAvailableForUser) {
                if (iw.isAgendaWritable(a, cookies)) {
                    writableAgendasForUser.add(a);
                }
            }
            SharedPreferences.Editor agendasEdit = agendas.edit();
            String json = gson.toJson(writableAgendasForUser);
            agendasEdit.putString("writableAgendas", json);
            agendasEdit.commit();
        }
        return writableAgendasForUser;
    }

    /**
     * Gets chosen primary color
     *
     * @return          Primary color
     */
    protected int getColorPrimary() {
        SharedPreferences themePref = context.getSharedPreferences("theme", Context.MODE_PRIVATE);
        return themePref.getInt("colorPrimary", ContextCompat.getColor(context, R.color.colorPrimary));
    }

    /**
     * Gets darker primary color
     *
     * @return          Dark primary color
     */
    protected int getColorPrimaryDark() {
        float[] hsv = new float[3];
        int color = getColorPrimary();
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        return Color.HSVToColor(hsv);
    }

    /**
     * Gets chosen accent color
     *
     * @return          Accent color
     */
    protected int getColorAccent() {
        SharedPreferences themePref = context.getSharedPreferences("theme", Context.MODE_PRIVATE);
        return themePref.getInt("colorAccent", ContextCompat.getColor(context, R.color.colorAccent));
    }

    /**
     * Gets chosen color for past events
     *
     * @return          Past events color
     */
    protected int getColorPast() {
        SharedPreferences themePref = context.getSharedPreferences("theme", Context.MODE_PRIVATE);
        return themePref.getInt("colorPast", ContextCompat.getColor(context, R.color.colorAccent));
    }

    /**
     * Gets chosen color for future events
     *
     * @return          Future events color
     */
    protected int getColorFuture() {
        SharedPreferences themePref = context.getSharedPreferences("theme", Context.MODE_PRIVATE);
        return themePref.getInt("colorFuture", ContextCompat.getColor(context, R.color.colorAccent));
    }

    protected boolean isEventDone(Event e) {
        SharedPreferences eventsDone = context.getSharedPreferences("done", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = eventsDone.getString("eventsDone", null);
        Type type = new TypeToken<Set<Event>>(){}.getType();
        Set<Event> doneEvents = gson.fromJson(json, type);
        if (doneEvents != null) {
            for (Event ed : doneEvents) {
                if (ed.getText().equals(e.getText())) {
                    if (ed.getDate().equals(e.getDate())) {
                        if (ed.getAgenda().getId().equals(e.getAgenda().getId())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected void setEventStatus(Event e, boolean done) {
        if (isEventDone(e) == done) {
            return;
        }
        SharedPreferences eventsDone = context.getSharedPreferences("done", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String eventsJSON = eventsDone.getString("eventsDone", null);
        Type eventSetType = new TypeToken<Set<Event>>(){}.getType();
        Set<Event> doneEvents = gson.fromJson(eventsJSON, eventSetType);
        if (doneEvents == null)
            doneEvents = new HashSet<>();

        if (done) {
            doneEvents.add(e);
        } else {
            Event etr = null;
            for (Event ed : doneEvents) {
                if (ed.getText().equals(e.getText())) {
                    etr = ed;
                }
            }
            if (etr != null) {
                doneEvents.remove(etr);
            }
        }
        String doneEventsJSON = gson.toJson(doneEvents);
        SharedPreferences.Editor eventsDoneEdit = eventsDone.edit();
        eventsDoneEdit.putString("eventsDone", doneEventsJSON);
        eventsDoneEdit.commit();
    }

    protected Set<Agenda> getUserAgendas(Map<String,String> cookies) {
        Set<Agenda> userAgendas = new HashSet<>();
        Gson gson = new Gson();
        SharedPreferences agendas = context.getSharedPreferences("agendas", Context.MODE_PRIVATE);
        String userAgendasJSON = agendas.getString("userAgendas", null);

        if (userAgendasJSON != null) {
            Type type = new TypeToken<Set<Agenda>>(){}.getType();
            userAgendas = gson.fromJson(userAgendasJSON, type);
        }
        if (userAgendas.isEmpty()) {
            userAgendas = iw.getAgendas(cookies);
            SharedPreferences.Editor agendasEdit = agendas.edit();
            String json = gson.toJson(userAgendas);
            agendasEdit.putString("userAgendas", json);
            agendasEdit.commit();
        }
        return userAgendas;
    }

    protected int getSyncMonths() {
        try {
        SharedPreferences generalSharedPref = context.getSharedPreferences("general", Context.MODE_PRIVATE);
        return generalSharedPref.getInt("calendarMonths", 2);
        } catch (Exception e) {
            e.printStackTrace();
            return 2;
        }
    }

    protected Set<Agenda> getUserSelectedAgendas() {
        SharedPreferences agendaPref = context.getSharedPreferences("agendas", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = agendaPref.getString("selectedAgendas", null);
        Type type = new TypeToken<Set<Agenda>>(){}.getType();

        return gson.fromJson(json, type); // selectedAgendas
    }

    /**
     * You guessed it. Checks if Internet connection is available
     *
     * @return  True if Internet connection is available, otherwise false.
     */
    protected boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
