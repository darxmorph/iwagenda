package com.henry.iwagenda;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.timessquare.CalendarCellDecorator;
import com.squareup.timessquare.CalendarCellView;
import com.squareup.timessquare.CalendarPickerView;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    final iwAPI iw = new iwAPI();
    private BroadcastReceiver themeChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences generalSharedPref = MainActivity.this.getSharedPreferences("general", Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), AddToIWActivity.class));
            }
        });

        paintUI();

        // Calendar init
        Calendar future = Calendar.getInstance();
        future.add(Calendar.MONTH, generalSharedPref.getInt("months", 2) - 1);
        future.set(Calendar.DATE, future.getActualMaximum(Calendar.DATE));

        Calendar present = Calendar.getInstance();
        present.set(Calendar.DATE, present.getActualMinimum(Calendar.DATE));
        CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
        iwcalendar.init(present.getTime(), future.getTime());

        // Fill calendar
        int months = generalSharedPref.getInt("months", 2);
        Set<Agenda> agendas = askUserToChooseAgendas(LoginActivity.cookiejar);
        if (agendas != null)
            calendarStuff(agendas, LoginActivity.cookiejar, months);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(getBaseContext(),SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onDestroy() {
        unregisterReceiver(themeChangeReceiver);
        super.onDestroy();
    }

    /**
     * Asks user to choose agendas
     * Shows an AlertDialog to select agendas, based on the names
     * If they were already chosen, returns a Set with the selected agendas
     *
     * @param  cookies  Session cookies (obtained after login)
     * @return          Selected agendas (Set) / Saved agendas (Set)
     */
    private Set<Agenda> askUserToChooseAgendas(final Map<String,String> cookies) {
        final SharedPreferences generalSharedPref = MainActivity.this.getSharedPreferences("general", Context.MODE_PRIVATE);
        if (!generalSharedPref.contains("selectedAgendas")) {
            MaterialDialog.Builder pleaseWaitBuilder = new MaterialDialog.Builder(MainActivity.this)
                    .title(getText(R.string.please_wait))
                    .content(getText(R.string.getting_things_ready))
                    .progress(true, 0);

            SharedPreferences themePref = MainActivity.this.getSharedPreferences("theme", Context.MODE_PRIVATE);
            if (themePref.contains("colorAccent"))
                pleaseWaitBuilder.widgetColor(themePref.getInt("colorAccent", ContextCompat.getColor(MainActivity.this, R.color.colorAccent)));

            final MaterialDialog pleaseWait = pleaseWaitBuilder.build();

            pleaseWait.setCancelable(false);
            pleaseWait.setCanceledOnTouchOutside(false);
            pleaseWait.show();

            new AsyncTask<Void, Void, Set<Agenda>>(){
                @Override
                protected Set<Agenda> doInBackground(Void... params) {
                    return iw.getAgendas(cookies);
                }

                @Override
                protected void onPostExecute(Set<Agenda> result) {
                    final Set<Agenda> agendas = result;
                    final Set<String> agendaNameSet = new HashSet<>();
                    for (Agenda a : agendas) {
                        agendaNameSet.add(a.getName());
                    }
                    final CharSequence[] agendaNames = agendaNameSet.toArray(new CharSequence[agendas.size()]);
                    final Set<Agenda> selectedAgendas = new HashSet<>();
                    final boolean[] selected = new boolean[agendas.size()];

                    pleaseWait.dismiss();

                    final AlertDialog askagendas = new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.choose_agendas)
                            .setMultiChoiceItems(agendaNames, selected, new DialogInterface.OnMultiChoiceClickListener() {
                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                    selected[which] = isChecked;
                                }
                            })
                            .setPositiveButton(R.string.ok, null)
                            .create();

                    View.OnClickListener sav = new View.OnClickListener() {
                        public void onClick (View v) {
                            if (!(agendaNameSet.size() == agendaNames.length && agendaNames.length == selected.length))
                                throw new UnknownError("All variables should be the same length/size");

                            selectedAgendas.clear();

                            for (int i = 0; i < agendaNames.length; i++) {
                                if (selected[i]) {
                                    for (Agenda a : agendas) {
                                        if (a.getName() == agendaNames[i]) {
                                            selectedAgendas.add(a);
                                        }
                                    }
                                }
                            }
                            if (selectedAgendas.size() < 1) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(R.string.no_agenda_selected)
                                        .setMessage(R.string.select_one_agenda_at_least)
                                        .setNegativeButton(R.string.ok, null)
                                        .show();
                            } else if (selectedAgendas.size() > 2) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(R.string.too_many_agendas)
                                        .setMessage(R.string.app_performance_data)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                SharedPreferences.Editor sharedPrefEdit = generalSharedPref.edit();
                                                Gson gson = new Gson();
                                                String json = gson.toJson(selectedAgendas);
                                                sharedPrefEdit.putString("selectedAgendas", json);
                                                sharedPrefEdit.commit();
                                                askagendas.dismiss();
                                                int months = generalSharedPref.getInt("months", 2);
                                                calendarStuff(agendas, LoginActivity.cookiejar, months);
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, null)
                                        .show();
                            } else {
                                SharedPreferences.Editor sharedPrefEdit = generalSharedPref.edit();
                                Gson gson = new Gson();
                                String json = gson.toJson(selectedAgendas);
                                sharedPrefEdit.putString("selectedAgendas", json);
                                sharedPrefEdit.commit();
                                askagendas.dismiss();
                                int months = generalSharedPref.getInt("months", 2);
                                calendarStuff(agendas, LoginActivity.cookiejar, months);
                            }
                        }
                    };

                    askagendas.setCanceledOnTouchOutside(false);
                    askagendas.setCancelable(false);
                    askagendas.show();
                    askagendas.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(sav);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            return null;
        } else {
            Gson gson = new Gson();
            String json = generalSharedPref.getString("selectedAgendas", null);
            Type type = new TypeToken<Set<Agenda>>(){}.getType();

            return gson.fromJson(json, type); // selectedAgendas
        }
    }

    /**
     * Does calendar stuff
     * Sets calendar cell background based on the events
     * and also contains the OnDateSelectedListener for the calendar
     *
     * @param  agendas  Set of Agenda to look for events
     * @param  cookies  Session cookies (obtained after login)
     */
    private void calendarStuff(final Set<Agenda> agendas, final Map<String,String> cookies, final int months) {
        final CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
        final SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);

        srl.setRefreshing(true);
        refreshCalendarEvents refreshCalendarEvents = new refreshCalendarEvents(agendas, cookies, months);
        refreshCalendarEvents.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshCalendarEvents refreshCalendarEvents = new refreshCalendarEvents(agendas, cookies, months);
                refreshCalendarEvents.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        iwcalendar.setOnDateSelectedListener(new CalendarPickerView.OnDateSelectedListener() {
            @Override
            public void onDateSelected(final Date date) {

                MaterialDialog.Builder eventBuilder = new MaterialDialog.Builder(MainActivity.this)
                        .title(getText(R.string.homework) + " " + iwAPI.iwDateFormats.date.format(date))
                        .content(getText(R.string.please_wait))
                        .iconRes(R.drawable.ic_pencil)
                        .progress(true, 0);

                SharedPreferences themePref = MainActivity.this.getSharedPreferences("theme", Context.MODE_PRIVATE);
                if (themePref.contains("colorAccent"))
                    eventBuilder.widgetColor(themePref.getInt("colorAccent", ContextCompat.getColor(MainActivity.this, R.color.colorAccent)));

                final MaterialDialog event = eventBuilder.build();

                event.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;

                event.setCanceledOnTouchOutside(false);
                event.setCancelable(false);

                event.show();

                new AsyncTask<String, Void, Set<Event>>(){
                    @Override
                    protected Set<Event> doInBackground(String... params) {
                        return iw.getEventsForDate(agendas,date,cookies);
                    }

                    @Override
                    protected void onPostExecute(Set<Event> result) {
                        String messageToShow = "";
                        if (!result.isEmpty()) {
                            for (Event e : result) {
                                messageToShow += e.getText();
                                messageToShow += "\n";
                                messageToShow += "\n";
                            }
                            messageToShow = messageToShow.substring(0, messageToShow.length() - 2);
                        } else {
                            messageToShow = getText(R.string.nohomework) + " " + iwAPI.iwDateFormats.date.format(date);
                        }
                        // pleaseWait.getProgressBar().setVisibility(View.GONE);
                        ((ViewManager)event.getProgressBar().getParent()).removeView(event.getProgressBar());
                        event.getContentView().setPadding(0, 0, 0, 0);
                        event.setCancelable(true);
                        event.setCanceledOnTouchOutside(true);
                        event.getContentView().setText(messageToShow);
                        // TextView messageView = (TextView) event.findViewById(android.R.id.message);
                        // messageView.setText(messageToShow);
                        // event.setMessage(messageToShow);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            @Override
            public void onDateUnselected(Date date) {}
        });
    }

    private class refreshCalendarEvents extends AsyncTask<String, Void, List<CalendarCellDecorator>> {
        final Set<Agenda> agendas;
        final Map<String,String> cookies;
        final int months;

        refreshCalendarEvents(Set<Agenda> agendas, Map<String,String> cookies, final int months) {
            this.agendas = agendas;
            this.cookies = cookies;
            this.months = months;
        }

        @Override
        protected List<CalendarCellDecorator> doInBackground(String... params) {
            SharedPreferences generalSharedPref = MainActivity.this.getSharedPreferences("theme", Context.MODE_PRIVATE);
            List<CalendarCellDecorator> decoratorList = new ArrayList<>();
            int colorBefore = generalSharedPref.getInt("colorBefore", ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
            int colorAfter = generalSharedPref.getInt("colorAfter", ContextCompat.getColor(MainActivity.this,R.color.colorAccent));
            for (Date d : iw.getEventDateSet(agendas, cookies, months)) {
                if (d.after(new Date())) {
                    decoratorList.add(new EventDecorator(d,colorAfter));
                } else {
                    decoratorList.add(new EventDecorator(d,colorBefore));
                }
            }
            return decoratorList;
        }

        @Override
        protected void onPostExecute(List<CalendarCellDecorator> result) {
            CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
            SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
            iwcalendar.setDecorators(result);
            srl.setRefreshing(false);
        }
    }

    public class EventDecorator implements CalendarCellDecorator {
        private Date evd;
        private int BackgroundColor;
        public EventDecorator (Date evd, int BackgroundColor) {
            this.evd = evd;
            this.BackgroundColor = BackgroundColor;
        }

        @Override
        public void decorate(CalendarCellView calendarCellView, Date date) {
            if(date.equals(evd)) {
                calendarCellView.setBackgroundColor(BackgroundColor);
            }
        }
    }

    private void paintUI() {
        tintUI();
        // Receive theme change events
        IntentFilter filter = new IntentFilter();
        filter.addAction("THEME_CHANGED");

        themeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tintUI();
            }
        };

        registerReceiver(themeChangeReceiver, filter);
    }

    private void tintUI() {
        SharedPreferences themePref = MainActivity.this.getSharedPreferences("theme", Context.MODE_PRIVATE);
        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (themePref.contains("colorPrimary"))
            toolbar.setBackgroundColor(themePref.getInt("colorPrimary", ContextCompat.getColor(MainActivity.this, R.color.colorPrimary)));
        // FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (themePref.contains("colorAccent"))
            fab.setBackgroundTintList(ColorStateList.valueOf(themePref.getInt("colorAccent", ContextCompat.getColor(MainActivity.this, R.color.colorAccent))));
        // SwipeRefreshLayout
        SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
        if (themePref.contains("colorAccent"))
            srl.setColorSchemeColors(themePref.getInt("colorAccent", ContextCompat.getColor(MainActivity.this, R.color.colorAccent)));
    }
}
