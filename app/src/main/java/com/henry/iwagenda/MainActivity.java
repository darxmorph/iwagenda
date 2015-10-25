package com.henry.iwagenda;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.timessquare.CalendarCellDecorator;
import com.squareup.timessquare.CalendarCellView;
import com.squareup.timessquare.CalendarPickerView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
        checkIDset();
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

    private void showAlert(String title, String text) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(text)
                .setIcon(android.R.drawable.ic_menu_agenda)
                .show();
    }
    private void checkIDset() {
        final SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("general", Context.MODE_PRIVATE);

        if (!sharedPref.contains("iwaid")) {
            final MaterialEditText inid = new MaterialEditText(this);
            inid.setInputType(InputType.TYPE_CLASS_NUMBER);
            final DialogInterface.OnClickListener inYes = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String id = inid.getText().toString();
                    SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
                    sharedPrefEdit.putString("iwaid",id);
                    sharedPrefEdit.commit();
                    callSync(id);
                }
            };
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.aid)
                    .setMessage(R.string.prompt_id)
                    .setView(inid)
                    .setPositiveButton(R.string.ok,inYes)
                    .setIcon(android.R.drawable.ic_menu_agenda)
                    .show();
            } else {
            callSync(sharedPref.getString("iwaid", null));
        }
        }

    private void callSync(String id) {
        parseIWagenda mUpdateTask;
        final SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("auth", Context.MODE_PRIVATE);
        mUpdateTask = new parseIWagenda();
        Calendar present = Calendar.getInstance();
        // TODO: use 'Personal' daid=0 (in Settings)
        // TODO: get multiple ids
		// TODO: Put this in settings/login to make app more general?
        final String iwURL = "http://agora.xtec.cat/escolapuigcerver/intranet/index.php?module=IWAgendas&any=" + present.get(Calendar.YEAR) + "&llistat=1&daid=" + id;
        // mUpdateTask.execute(iwURL + "&mes=" + (present.get(Calendar.MONTH) + 1),"ZKSID242",sharedPref.getString("iwcookie",null));
        mUpdateTask.execute(iwURL,"ZKSID242",sharedPref.getString("iwcookie",null));
        CalendarPickerView iwcalendar = (CalendarPickerView) findViewById(R.id.iwcalendar);
        iwcalendar.setOnDateSelectedListener(new CalendarPickerView.OnDateSelectedListener() {
            @Override
            public void onDateSelected(Date date) {
                DateFormat dfd = new SimpleDateFormat("dd");
                DateFormat dfm = new SimpleDateFormat("MM");
                DateFormat dfy = new SimpleDateFormat("yy");
                parseIWday mParseDayTask;
                mParseDayTask = new parseIWday(dfd.format(date) + "/" + dfm.format(date) + "/" + dfy.format(date));
                mParseDayTask.execute(iwURL + "&mes=" + dfm.format(date) + "&dia=" + dfd.format(date),"ZKSID242",sharedPref.getString("iwcookie",null));
            }
            @Override
            public void onDateUnselected(Date date) {}
        });
    }

    class parseIWagenda extends AsyncTask<String, Void, Elements> {
        @Override
        protected Elements doInBackground(String... params) {
            Elements iwe = new Elements();

            try {
                // TODO: use for... (with params)
                Calendar present = Calendar.getInstance();
                Document iw1 = Jsoup.connect(params[0] + "&mes=" + (present.get(Calendar.MONTH) + 1)).cookie(params[1], params[2]).get();
                Document iw2 = Jsoup.connect(params[0] + "&mes=" + (present.get(Calendar.MONTH) + 2)).cookie(params[1], params[2]).get();
                Document iw3 = Jsoup.connect(params[0] + "&mes=" + (present.get(Calendar.MONTH) + 3)).cookie(params[1], params[2]).get();
                Elements events1 = iw1.getElementsByAttributeValueStarting("id", "note_");
                Elements events2 = iw2.getElementsByAttributeValueStarting("id", "note_");
                Elements events3 = iw3.getElementsByAttributeValueStarting("id", "note_");
                iwe.addAll(events1);
                iwe.addAll(events2);
                iwe.addAll(events3);
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
                    Date dr =  df.parse(element.text());
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
        parseIWday(String evday) {
            mevday = evday;
        }
        @Override
        protected Elements doInBackground(String... params) {
            Elements iwe = new Elements();

            try {
                Document iw = Jsoup.connect(params[0]).cookie(params[1],params[2]).get();
                Elements events = iw.getElementsByAttributeValueStarting("id", "note_");
                iwe = events;
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
                showAlert(getText(R.string.homework) + " " + mevday,res);
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
