package com.henry.iwagenda;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.squareup.timessquare.CalendarCellDecorator;
import com.squareup.timessquare.CalendarCellView;
import com.squareup.timessquare.CalendarPickerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    final UserResources ur = new UserResources(this);
    final Offline off = new Offline(this);

    private BroadcastReceiver themeChangeReceiver;
    private BroadcastReceiver offlineSyncCompleteReceiver;
    private BroadcastReceiver calendarColorChangeReceiver;
    private BroadcastReceiver calendarMonthsChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LoginActivity.cookiejar == null) {
                    if (ur.isConnectedToInternet()) {
                        Snackbar.make(v, getText(R.string.internet_not_connected), Snackbar.LENGTH_LONG).setAction(getText(R.string.retry), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);
                            }
                        }).show();
                    } else {
                        Snackbar.make(v, getText(R.string.internet_not_connected), Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    startActivity(new Intent(getBaseContext(), AddToIWActivity.class));
                }

            }
        });

        setReceivers();

        if (LoginActivity.cookiejar == null) {
            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setTitle(bar.getTitle() + " (Offline)");
            }
        }

        // off.startSyncService();

        // Calendar init
        int calendarMonths = ur.getSyncMonths();
        Calendar future = Calendar.getInstance();
        future.add(Calendar.MONTH, calendarMonths - 1);
        future.set(Calendar.DATE, future.getActualMaximum(Calendar.DATE));

        Calendar present = Calendar.getInstance();
        present.set(Calendar.DATE, present.getActualMinimum(Calendar.DATE));

        CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
        iwcalendar.init(present.getTime(), future.getTime());

        // Fill calendar
        Set<Agenda> agendas = askUserToChooseAgendas(LoginActivity.cookiejar);

        if (agendas != null)
            calendarStuff(agendas, LoginActivity.cookiejar, false);

        if (LoginActivity.cookiejar != null)
            off.syncIfNeeded();
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
        unregisterReceiver(calendarMonthsChangeReceiver);
        unregisterReceiver(calendarColorChangeReceiver);
        unregisterReceiver(offlineSyncCompleteReceiver);
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
        final Set<Agenda> selectedAgendas = ur.getUserSelectedAgendas();
        if (selectedAgendas == null || selectedAgendas.isEmpty()) {
            MaterialDialog.Builder pleaseWaitBuilder = new MaterialDialog.Builder(this)
                    .title(getText(R.string.please_wait))
                    .content(getText(R.string.getting_things_ready))
                    .progress(true, 0);

            pleaseWaitBuilder.widgetColor(ur.getColorAccent());

            final MaterialDialog pleaseWait = pleaseWaitBuilder.build();

            pleaseWait.setCancelable(false);
            pleaseWait.setCanceledOnTouchOutside(false);
            pleaseWait.show();

            final SharedPreferences agendaPref = getSharedPreferences("agendas", Context.MODE_PRIVATE);
            final SharedPreferences.Editor agendaEdit = agendaPref.edit();

            new AsyncTask<Void, Void, Set<Agenda>>(){
                @Override
                protected Set<Agenda> doInBackground(Void... params) {
                    return ur.getUserAgendas(cookies);
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
                                                Gson gson = new Gson();
                                                String json = gson.toJson(selectedAgendas);
                                                agendaEdit.putString("selectedAgendas", json);
                                                agendaEdit.commit();
                                                askagendas.dismiss();
                                                calendarStuff(agendas, LoginActivity.cookiejar, true);
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, null)
                                        .show();
                            } else {
                                Gson gson = new Gson();
                                String json = gson.toJson(selectedAgendas);
                                agendaEdit.putString("selectedAgendas", json);
                                agendaEdit.commit();
                                askagendas.dismiss();
                                calendarStuff(agendas, LoginActivity.cookiejar, true);
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
            return selectedAgendas;
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
    private void calendarStuff(final Set<Agenda> agendas, final Map<String,String> cookies, boolean resyncFromIW) {
        final CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
        final SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);

        srl.setRefreshing(true);
        new refreshCalendarEvents(agendas, cookies, resyncFromIW).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (LoginActivity.cookiejar == null) {
                    if (ur.isConnectedToInternet()) {
                        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                        Snackbar.make(fab, getText(R.string.internet_not_connected), Snackbar.LENGTH_LONG).setAction(getText(R.string.retry), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);
                            }
                        }).show();
                        srl.setRefreshing(false);
                    } else {
                        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                        Snackbar.make(fab, getText(R.string.internet_not_connected), Snackbar.LENGTH_LONG).show();
                        srl.setRefreshing(false);
                    }
                } else {
                    new refreshCalendarEvents(agendas, cookies, true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });

        iwcalendar.setOnDateSelectedListener(new CalendarPickerView.OnDateSelectedListener() {
            @Override
            public void onDateSelected(final Date date) {
                MaterialDialog.Builder eventBuilder = new MaterialDialog.Builder(MainActivity.this)
                        .title(getText(R.string.homework) + " " + iwAPI.iwDateFormats.date.format(date))
                        .iconRes(R.drawable.ic_pencil);

                final Set<Event> offlineEventsForDate = off.getOfflineEventsForDate(date);

                if (offlineEventsForDate.isEmpty()) {
                    eventBuilder.content(getText(R.string.nohomework) + " " + iwAPI.iwDateFormats.date.format(date));

                    final MaterialDialog event = eventBuilder.build();
                    event.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
                    event.show();
                } else {
                    Set<String> homeworkStringSet = new HashSet<>();

                    for (Event e : offlineEventsForDate) {
                        homeworkStringSet.add(e.getText());
                    }
                    String[] homeworkarray = homeworkStringSet.toArray(new String[homeworkStringSet.size()]);
                    eventBuilder.items(homeworkarray);

                    final MaterialDialog event = eventBuilder.build();
                    event.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
                    event.show();

                    final ListView eventListView = event.getListView();

                    if (eventListView != null) {
                        eventListView.post(new Runnable() {
                            @Override
                            public void run() {
                                for (Event e : offlineEventsForDate) {
                                    if (ur.isEventDone(e)) {
                                        for (int i = 0; i < eventListView.getChildCount(); i++) {
                                            LinearLayout ll = (LinearLayout) eventListView.getChildAt(i);
                                            TextView tv = (TextView) ll.getChildAt(0);
                                            if (e.getText() == tv.getText()) {
                                                tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }

                    if (eventListView != null) {
                        eventListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                                LinearLayout ll = (LinearLayout) view;

                                TextView tv = (TextView) ll.getChildAt(0);

                                if ((tv.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) == Paint.STRIKE_THRU_TEXT_FLAG) {
                                    tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                                } else {
                                    tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                }

                                for (Event e : offlineEventsForDate) {
                                    if (e.getText() == tv.getText()) {
                                        ur.setEventStatus(e, (tv.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) == Paint.STRIKE_THRU_TEXT_FLAG);
                                    }
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onDateUnselected(Date date) {
            }
        });
    }

    private class refreshCalendarEvents extends AsyncTask<String, Void, List<CalendarCellDecorator>> {
        final Set<Agenda> agendas;
        final Map<String,String> cookies;
        final boolean resyncFromIW;

        refreshCalendarEvents(Set<Agenda> agendas, Map<String,String> cookies, boolean resyncFromIW) {
            this.agendas = agendas;
            this.cookies = cookies;
            this.resyncFromIW = resyncFromIW;
        }

        @Override
        protected List<CalendarCellDecorator> doInBackground(String... params) {
            List<CalendarCellDecorator> decoratorList = new ArrayList<>();
            int colorPast = ur.getColorPast();
            int colorFuture = ur.getColorFuture();
            Set<Date> dates;

            if (resyncFromIW) {
                off.startSyncService();
                return null;
            }

            dates = off.getOfflineEventsDates();
            decoratorList.add(new EventDecorator(dates, colorFuture, colorPast));
            return decoratorList;
        }

        @Override
        protected void onPostExecute(List<CalendarCellDecorator> result) {
            if (result != null) {
                CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
                SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
                iwcalendar.setDecorators(result);
                srl.setRefreshing(false);
            }
        }
    }

    public class EventDecorator implements CalendarCellDecorator {
        private Set<Date> eventDates;
        private int futureEventBackgroundColor;
        private int pastEventBackgroundColor;
        public EventDecorator (Set<Date> eventDates, int futureEventBackgroundColor, int pastEventBackgroundColor) {
            this.eventDates = eventDates;
            this.futureEventBackgroundColor = futureEventBackgroundColor;
            this.pastEventBackgroundColor = pastEventBackgroundColor;
        }

        @Override
        public void decorate(CalendarCellView calendarCellView, Date date) {
            if (eventDates.contains(date)) {
                if (date.after(new Date())) {
                    calendarCellView.setBackgroundColor(futureEventBackgroundColor);
                } else {
                    calendarCellView.setBackgroundColor(pastEventBackgroundColor);
                }
            } else {
                calendarCellView.setBackgroundResource(R.drawable.calendar_bg_selector);
            }
        }
    }

    private void setReceivers() {
        tintUI();
        // Receive sync complete events
        IntentFilter syncFilter = new IntentFilter();
        syncFilter.addAction("OFFLINE_SYNC_COMPLETE");

        offlineSyncCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new refreshCalendarEvents(ur.getUserSelectedAgendas(), LoginActivity.cookiejar, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        };

        registerReceiver(offlineSyncCompleteReceiver, syncFilter);

        // Receive theme change events
        IntentFilter themeFilter = new IntentFilter();
        themeFilter.addAction("THEME_CHANGE");

        themeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tintUI();
            }
        };

        registerReceiver(themeChangeReceiver, themeFilter);

        // Receive calendar color change events
        IntentFilter calendarColorFilter = new IntentFilter();
        calendarColorFilter.addAction("CALENDAR_COLOR_CHANGE");

        calendarColorChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new refreshCalendarEvents(ur.getUserAgendas(LoginActivity.cookiejar), LoginActivity.cookiejar, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        };

        registerReceiver(calendarColorChangeReceiver, calendarColorFilter);

        // Receive calendar months change events
        IntentFilter calendarMonthsFilter = new IntentFilter();
        calendarMonthsFilter.addAction("CALENDAR_MONTHS_CHANGE");

        calendarMonthsChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int calendarMonths = ur.getSyncMonths();
                Calendar future = Calendar.getInstance();
                future.add(Calendar.MONTH, calendarMonths - 1);
                future.set(Calendar.DATE, future.getActualMaximum(Calendar.DATE));

                Calendar present = Calendar.getInstance();
                present.set(Calendar.DATE, present.getActualMinimum(Calendar.DATE));

                CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
                iwcalendar.init(present.getTime(), future.getTime());

                if (LoginActivity.cookiejar != null) {
                    off.startSyncService();
                }
            }
        };

        registerReceiver(calendarMonthsChangeReceiver, calendarMonthsFilter);
    }

    private void tintUI() {
        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ur.getColorPrimary());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ur.getColorPrimaryDark());
            // window.setNavigationBarColor(ur.getColorPrimaryDark());
        }
        // FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(ur.getColorAccent()));
        // SwipeRefreshLayout
        SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
        srl.setColorSchemeColors(ur.getColorAccent());
    }
}
