package com.henry.iwagenda;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.timessquare.CalendarCellDecorator;
import com.squareup.timessquare.CalendarCellView;
import com.squareup.timessquare.CalendarPickerView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
        final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(),SettingsActivity.class));
            }
        };
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), AddToIWActivity.class));
            }
        });
        // Calendar init
        Calendar future = Calendar.getInstance();
        future.add(Calendar.MONTH, 2);

        CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
        iwcalendar.init(Calendar.getInstance().getTime(), future.getTime());
        chooseAgenda(false,null);
        // Snackbar.make(v, "Coming soon...", Snackbar.LENGTH_LONG).setAction("Action", null).show();
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

    private void chooseAgenda(boolean hasMap, final Map<String,String> agendes) {
        final SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("general", Context.MODE_PRIVATE);
        final SharedPreferences auth = MainActivity.this.getSharedPreferences("auth", Context.MODE_PRIVATE);
        final String iwURL = "http://agora.xtec.cat/escolapuigcerver/intranet/index.php?module=IWAgendas&any=" + Calendar.getInstance().get(Calendar.YEAR) + "&llistat=1";
        final String cookieName = "ZKSID242";
        final String cookieValue = auth.getString("iwcookie",null);
        if (!sharedPref.contains("iwas")) {
            if (hasMap) {
                final CharSequence[] items = agendes.keySet().toArray(new CharSequence[agendes.size()]);
                final Set<String> agsel = sharedPref.getStringSet("iwas", new HashSet<String>());
                boolean[] selected = new boolean[agendes.size()];
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Escolliu les agendes per obtenir les anotacions")
                        .setMultiChoiceItems(items, selected, new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialogInterface, int item, boolean b) {
                                for (String s : agendes.keySet()) {
                                    if (s == items[item]) {
                                        if (b) {
                                            agsel.add(agendes.get(s));
                                        } else {
                                            agsel.remove(agendes.get(s));
                                        }
                                    }
                                }
                            }
                        })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
                                if (sharedPref.contains("iwas")) {
                                    sharedPrefEdit.remove("iwas");
                                    sharedPrefEdit.commit();
                                }
                                sharedPrefEdit.putStringSet("iwas", agsel);
                                sharedPrefEdit.commit();
                                callSync(agsel, cookieName, cookieValue,iwURL);
                            }
                        })
                        .show();
            } else {
                new getAgendaList().execute(iwURL,cookieName,cookieValue);
            }
        } else {
            callSync(sharedPref.getStringSet("iwas",null),cookieName,cookieValue,iwURL);
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
            final Map<String,String> agendes = new HashMap<>();
            for (Element agenda : result) {
                String agtitle = agenda.attr("title");
                String agurl = agenda.attr("href");
                String id = agurl.substring(agurl.lastIndexOf("&daid=") + 6);
                agtitle = agtitle.trim().replaceAll(" +", " ");
                if (!(agtitle.equals("Personal"))) {
                    agendes.put(agtitle,id);
                }
            }
            chooseAgenda(true,agendes);
        }
    }

    private void showAlert(String title, String text) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(text)
                .setIcon(R.drawable.ic_pencil)
                .show();
    }
    private boolean[] toPrimitiveArray(final List<Boolean> booleanList) {
        final boolean[] primitives = new boolean[booleanList.size()];
        int index = 0;
        for (Boolean object : booleanList) {
            primitives[index++] = object;
        }
        return primitives;
    }

    private void callSync(final Set<String> agendes, final String cookieName, final String cookieValue, final String iwURL) {
        new parseIWagenda(agendes).execute(iwURL, cookieName, cookieValue);
        CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
        iwcalendar.setOnDateSelectedListener(new CalendarPickerView.OnDateSelectedListener() {
            @Override
            public void onDateSelected(Date date) {
                DateFormat dfd = new SimpleDateFormat("dd");
                DateFormat dfm = new SimpleDateFormat("MM");
                DateFormat dfy = new SimpleDateFormat("yy");
                new parseIWday(dfd.format(date) + "/" + dfm.format(date) + "/" + dfy.format(date),agendes)
                        .execute(iwURL + "&mes=" + dfm.format(date) + "&dia=" + dfd.format(date), cookieName, cookieValue);
            }
            @Override
            public void onDateUnselected(Date date) {}
        });
    }

    class parseIWagenda extends AsyncTask<String, Void, Elements> {
        private final Set<String> mAgendes;
        parseIWagenda (Set<String> agendes) {
            mAgendes = agendes;
        }

        @Override
        protected Elements doInBackground(String... params) {
            Elements iwe = new Elements();

            try {
                Calendar present = Calendar.getInstance();
                int months = 3;
                for (int i=0;i<months;i++) {
                    for (String a : mAgendes) {
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
            decoratorList.add(new EventDecorator(MainActivity.this,new Date()));
            CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);

            for (Element element : result) {
                try {
                    DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                    Date dr = df.parse(element.text());
                    if(dr.after(new Date()))
                        decoratorList.add(new EventDecorator(MainActivity.this, dr));
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
        Set<String> mAgendes;
        parseIWday(String evday, Set<String> agendes) {
            mevday = evday;
            mAgendes = agendes;
        }

        @Override
        protected Elements doInBackground(String... params) {
            Elements iwe = new Elements();

            try {
                for (String a : mAgendes) {
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
        private Context context;
        private Date evd;
        public EventDecorator (Context context, Date evd) {
            this.context = context;
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
