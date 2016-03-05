package com.henry.iwagenda;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AddToIWActivity extends AppCompatActivity {
    static class data
    {
        private static String text = null;
        private static Date date = null;
        private static Agenda agenda = null;
    }
    final iwAPI iw = new iwAPI();
    private UserResources ur = new UserResources(this);
    private BroadcastReceiver themeChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupActionBar();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                MaterialEditText homework = (MaterialEditText) findViewById(R.id.homework);
                data.text = homework.getText().toString();
                if (data.agenda != null && data.date != null && !TextUtils.isEmpty(data.text)) {

                    MaterialDialog.Builder pleaseWaitBuilder = new MaterialDialog.Builder(AddToIWActivity.this)
                            .title(getText(R.string.please_wait))
                            .content(getText(R.string.please_wait))
                            .progress(true, 0);

                    SharedPreferences themePref = AddToIWActivity.this.getSharedPreferences("theme", Context.MODE_PRIVATE);
                    if (themePref.contains("colorAccent"))
                        pleaseWaitBuilder.widgetColor(themePref.getInt("colorAccent", R.color.colorAccent));

                    final MaterialDialog pleaseWait = pleaseWaitBuilder.build();
                    pleaseWait.setCancelable(false);
                    pleaseWait.setCanceledOnTouchOutside(false);
                    pleaseWait.show();

                    new AsyncTask<String, Void, Boolean>(){
                        @Override
                        protected Boolean doInBackground(String... params) {
                            return iw.sendEventToIW(new Event(data.text, data.date, true, data.agenda),LoginActivity.cookiejar);
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            pleaseWait.dismiss();
                            if (result) {
                                Snackbar.make(view, R.string.add_success, Snackbar.LENGTH_LONG).show();
                            } else {
                                Snackbar.make(view, R.string.add_fail, Snackbar.LENGTH_LONG).show();
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    Snackbar.make(view, R.string.fill_all, Snackbar.LENGTH_LONG).show();
                }
            }
        });
        Button mSetDateButton = (Button) findViewById(R.id.date);
        mSetDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectDate();
            }
        });
        Button mSetAgendaButton = (Button) findViewById(R.id.agenda);
        mSetAgendaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseAgenda();
            }
        });

        paintUI();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(themeChangeReceiver);
        super.onDestroy();
    }

    private void selectDate() {
        DatePickerDialog.OnDateSetListener odsl = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                try {
                    data.date = iwAPI.iwDateFormats.date.parse(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Button mSetDateButton = (Button) findViewById(R.id.date);
                mSetDateButton.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                }
        };
        Calendar now = Calendar.getInstance();
        DatePickerDialog dpd = DatePickerDialog.newInstance(
                odsl,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        SharedPreferences themePref = AddToIWActivity.this.getSharedPreferences("theme", Context.MODE_PRIVATE);
        if (themePref.contains("colorAccent"))
            dpd.setAccentColor(themePref.getInt("colorAccent", R.color.colorAccent));
        dpd.show(getFragmentManager(), "AddToIW_ChooseDate");

    }

    private void chooseAgenda() {
        MaterialDialog.Builder pleaseWaitBuilder = new MaterialDialog.Builder(AddToIWActivity.this)
                .title(getText(R.string.please_wait))
                .content(getText(R.string.getting_things_ready))
                .progress(true, 0);

        SharedPreferences themePref = AddToIWActivity.this.getSharedPreferences("theme", Context.MODE_PRIVATE);
        if (themePref.contains("colorAccent"))
            pleaseWaitBuilder.widgetColor(themePref.getInt("colorAccent", R.color.colorAccent));

        final MaterialDialog pleaseWait = pleaseWaitBuilder.build();
        pleaseWait.setCancelable(false);
        pleaseWait.setCanceledOnTouchOutside(false);
        pleaseWait.show();

        new AsyncTask<Void, Void, Set<Agenda>>(){
            @Override
            protected Set<Agenda> doInBackground(Void... params) {
                return ur.getWritableAgendasForUser(LoginActivity.cookiejar);
            }

            @Override
            protected void onPostExecute(Set<Agenda> result) {
                pleaseWait.dismiss();

                final Set<Agenda> agendas = result;
                Set<String> agendaNameSet = new HashSet<>();
                for (Agenda a : agendas) {
                    agendaNameSet.add(a.getName());
                }
                final CharSequence[] agendaNames = agendaNameSet.toArray(new CharSequence[agendas.size()]);
                final AlertDialog chooseAgenda = new AlertDialog.Builder(AddToIWActivity.this)
                        .setTitle(R.string.choose_agenda_add)
                        .setSingleChoiceItems(agendaNames, -1, null)
                        .setPositiveButton(R.string.ok, null)
                        .create();

                View.OnClickListener dco = new View.OnClickListener() {
                    @Override
                    public void onClick (View v) {
                        ListView lw = chooseAgenda.getListView();
                        if (lw.getCheckedItemCount() >= 1) {
                            Object chosenAgenda = lw.getAdapter().getItem(lw.getCheckedItemPosition());
                            for (Agenda a : agendas) {
                                if (a.getName() == chosenAgenda) {
                                    data.agenda = a;
                                    Button mSetAgendaButton = (Button) findViewById(R.id.agenda);
                                    mSetAgendaButton.setText(a.getName());
                                    chooseAgenda.dismiss();
                                }
                            }
                        } else {
                            new AlertDialog.Builder(AddToIWActivity.this)
                                    .setTitle(R.string.no_agenda_selected)
                                    .setMessage(R.string.must_choose_agenda)
                                    .setNegativeButton(R.string.ok, null)
                                    .show();
                        }
                    }
                };
                chooseAgenda.show();
                chooseAgenda.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(dco);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    private void paintUI() {
        tintUI();
        // Receive theme change events
        IntentFilter filter = new IntentFilter();
        filter.addAction("THEME_CHANGE");

        themeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tintUI();
            }
        };

        registerReceiver(themeChangeReceiver, filter);
    }
    private void tintUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ur.getColorPrimary());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ur.getColorPrimaryDark());
            // window.setNavigationBarColor(ur.getColorPrimaryDark());
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(ur.getColorAccent()));

        MaterialEditText homework = (MaterialEditText) findViewById(R.id.homework);
        homework.setPrimaryColor(ur.getColorAccent());
    }
}
