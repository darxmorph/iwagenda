package com.henry.iwagenda;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.squareup.timessquare.CalendarCellDecorator;
import com.squareup.timessquare.CalendarCellView;
import com.squareup.timessquare.CalendarPickerView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

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
                startActivity(new Intent(getBaseContext(), AddToIWActivity.class));
            }
        });
        // Calendar init
        Calendar future = Calendar.getInstance();
        future.add(Calendar.MONTH, MainActivity.this.getSharedPreferences("general", Context.MODE_PRIVATE).getInt("months", 2) - 1);
        future.set(Calendar.DATE, future.getActualMaximum(Calendar.DATE));

        Calendar present = Calendar.getInstance();
        present.set(Calendar.DATE, present.getActualMinimum(Calendar.DATE));

        CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
        iwcalendar.init(present.getTime(), future.getTime());
        chooseAgenda(false, null);
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

    private void chooseAgenda(boolean hasMap, final Map<String,String> agendas) {
        final SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("general", Context.MODE_PRIVATE);
        final SharedPreferences auth = MainActivity.this.getSharedPreferences("auth", Context.MODE_PRIVATE);
        final String iwURL = "http://agora.xtec.cat/escolapuigcerver/intranet/index.php?module=IWAgendas&any=" + Calendar.getInstance().get(Calendar.YEAR) + "&llistat=1";
        final String cookieName = "ZKSID242";
        final String cookieValue = auth.getString("iwcookie", null);
        final int months = sharedPref.getInt("months",2);
        if (!sharedPref.contains("iwas")) {
            if (hasMap) {
                final CharSequence[] items = agendas.keySet().toArray(new CharSequence[agendas.size()]);
                final Set<String> agsel = sharedPref.getStringSet("iwas", new HashSet<String>());
                boolean[] selected = new boolean[agendas.size()];
                final AlertDialog askagendas = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.choose_agendas)
                        .setMultiChoiceItems(items, selected, new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialogInterface, int item, boolean b) {
                                for (String s : agendas.keySet()) {
                                    if (s == items[item]) {
                                        if (b) {
                                            agsel.add(agendas.get(s));
                                        } else {
                                            agsel.remove(agendas.get(s));
                                        }
                                    }
                                }
                            }
                        })
                        .setPositiveButton(R.string.ok, null)
                        .create();
                View.OnClickListener checksave = new View.OnClickListener() {
                    public void onClick(View v) {
                        if (agsel.size() < 1) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(R.string.no_agenda_selected)
                                    .setMessage(R.string.select_one_agenda_at_least)
                                    .setNegativeButton(R.string.ok, null)
                                    .show();
                        } else if (agsel.size() > 2) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(R.string.too_many_agendas)
                                    .setMessage(R.string.app_performance_data)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
                                            sharedPrefEdit.putStringSet("iwas", agsel);
                                            sharedPrefEdit.commit();
                                            callSync(agsel, cookieName, cookieValue, iwURL, months);
                                            askagendas.dismiss();
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, null)
                                    .show();
                        } else {
                            SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
                            sharedPrefEdit.putStringSet("iwas", agsel);
                            sharedPrefEdit.commit();
                            callSync(agsel, cookieName, cookieValue, iwURL, months);
                            askagendas.dismiss();
                        }
                    }
                };
                askagendas.setCanceledOnTouchOutside(false);
                askagendas.show();
                askagendas.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(checksave);
            } else {
                new getAgendaList().execute(iwURL,cookieName,cookieValue);
            }
        } else {
            callSync(sharedPref.getStringSet("iwas",null),cookieName,cookieValue,iwURL,months);
        }
    }
    class getAgendaList extends AsyncTask<String, Void, Elements> {
        @Override
        protected Elements doInBackground(String... params) {
            Elements ev = new Elements();

            try {
                Document iw = Jsoup.connect(params[0])
                        .userAgent("jsoup")
                        .cookie(params[1],params[2])
                        .get();
                Calendar present = Calendar.getInstance();
                ev = iw.select("[href^=index.php?module=IWAgendas&mes=" + (present.get(Calendar.MONTH) + 1) + "&any=" + present.get(Calendar.YEAR) + "&daid=]");
            }

            catch(IOException e) {
                e.printStackTrace();
            }
            return ev;
        }

        @Override
        protected void onPostExecute(Elements result) {
            final Map<String,String> agendas = new HashMap<>();
            for (Element agenda : result) {
                String agtitle = agenda.attr("title");
                String agurl = agenda.attr("href");
                String id = agurl.substring(agurl.lastIndexOf("&daid=") + 6);
                agtitle = agtitle.trim().replaceAll(" +", " ");
                if (!(agtitle.equals("Personal"))) {
                    agendas.put(agtitle,id);
                }
            }
            chooseAgenda(true,agendas);
        }
    }

    private void showAlert(String title, String text) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(text)
                .setIcon(R.drawable.ic_pencil)
                .show();
    }

    private void callSync(final Set<String> agendas, final String cookieName, final String cookieValue, final String iwURL, final int months) {
        new parseIWagenda(agendas, months).execute(iwURL, cookieName, cookieValue);
        CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
        iwcalendar.setOnDateSelectedListener(new CalendarPickerView.OnDateSelectedListener() {
            @Override
            public void onDateSelected(Date date) {
                DateFormat dfd = new SimpleDateFormat("dd");
                DateFormat dfm = new SimpleDateFormat("MM");
                DateFormat dfy = new SimpleDateFormat("yy");
                new parseIWday(dfd.format(date) + "/" + dfm.format(date) + "/" + dfy.format(date),agendas)
                        .execute(iwURL + "&mes=" + dfm.format(date) + "&dia=" + dfd.format(date), cookieName, cookieValue);
            }
            @Override
            public void onDateUnselected(Date date) {}
        });
    }

    class parseIWagenda extends AsyncTask<String, Void, Elements> {
        private final Set<String> mAgendas;
        private final int mnths;
        parseIWagenda (Set<String> agendas, int months) {
            mAgendas = agendas;
            mnths = months;
        }

        @Override
        protected Elements doInBackground(String... params) {
            Elements iwe = new Elements();

            try {
                Calendar present = Calendar.getInstance();
                for (int i=0;i<mnths;i++) {
                    for (String a : mAgendas) {
                        Document iw = Jsoup.connect(params[0] + "&mes=" + (present.get(Calendar.MONTH) + i + 1) + "&daid=" + a).cookie(params[1], params[2]).userAgent("jsoup").get();
                        Elements ev = iw.getElementsByAttributeValueStarting("id", "note_");
                        iwe.addAll(ev);
                    }
                }
            }

            catch(Throwable t) {
                t.printStackTrace();
            }

            return iwe;
        }

        @Override
        protected void onPostExecute(Elements result) {
            List<CalendarCellDecorator> decoratorList = new ArrayList<>();
            decoratorList.add(new EventDecorator(new Date()));
            CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);

            for (Element element : result) {
                try {
                    DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                    Date dr = df.parse(element.text());
                    if(dr.after(new Date()))
                        decoratorList.add(new EventDecorator(dr));
                    df.format(dr);
                }
                catch(ParseException pe) {
                    pe.printStackTrace();
                }
            }
            iwcalendar.setDecorators(decoratorList);
        }
    }

    class parseIWday extends AsyncTask<String, Void, Elements> {
        String mevday;
        Set<String> mAgendas;
        parseIWday(String evday, Set<String> agendas) {
            mevday = evday;
            mAgendas = agendas;
        }

        @Override
        protected Elements doInBackground(String... params) {
            Elements iwe = new Elements();

            try {
                for (String a : mAgendas) {
                    Document iw = Jsoup.connect(params[0] + "&daid=" + a).cookie(params[1], params[2]).userAgent("jsoup").get();
                    Elements events = iw.getElementsByAttributeValueStarting("id", "note_");
                    iwe.addAll(events);
                }
            }

            catch(Throwable t) {
                t.printStackTrace();
            }

            return iwe;
        }

        @Override
        protected void onPostExecute(Elements result) {
            String res = new String();

            if (result.hasText()) {
                for (Element element : result) {
                    String tmpstr = element.text();
                    tmpstr = tmpstr.replace("Tot el dia", "");
                    tmpstr = tmpstr.replace("---", "");
                    tmpstr = tmpstr.substring(1);
                    tmpstr += "\r\n";
                    tmpstr += "\r\n";
                    res += tmpstr;
                }
                res = res.substring(0,res.length()-4);
                showAlert(getText(R.string.homework) + " " + mevday,res);
            } else {
                res = getText(R.string.nohomework) + " " + mevday;
                showAlert(getText(R.string.homework) + " " + mevday, res);
            }
        }
    }

    public class EventDecorator implements CalendarCellDecorator {
        private Date evd;
        public EventDecorator (Date evd) {
            this.evd = evd;
        }

        @Override
        public void decorate(CalendarCellView calendarCellView, Date date) {
            if(date.equals(evd)) {
                calendarCellView.setBackgroundResource(R.color.colorAccent);
            }
        }
    }
}
