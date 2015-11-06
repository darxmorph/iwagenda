package com.henry.iwagenda;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddToIWActivity extends AppCompatActivity {
    final Map<String,String> data = new HashMap<>();

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
            public void onClick(View view) {
                SharedPreferences auth = AddToIWActivity.this.getSharedPreferences("auth", Context.MODE_PRIVATE);
                String cookieName = "ZKSID242";
                String cookieValue = auth.getString("iwcookie", null);
                EditText homework = (EditText) findViewById(R.id.homework);
                String text = homework.getText().toString();
                if (data.containsKey("aID") && data.containsKey("dayOfMonth") && data.containsKey("monthOfYear") && data.containsKey("year") && !TextUtils.isEmpty(text)) {
                    String aID = data.get("aID");
                    String dayOfMonth = data.get("dayOfMonth");
                    String monthOfYear = data.get("monthOfYear");
                    String year = data.get("year");

                    sendNote(cookieName, cookieValue, aID, dayOfMonth, monthOfYear, year, text);
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
                selectAgenda(false, null);
            }
        });
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

    private void selectDate() {
        DatePickerDialog.OnDateSetListener odsl = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                data.put("dayOfMonth", Integer.toString(dayOfMonth));
                data.put("monthOfYear",Integer.toString(monthOfYear + 1));
                data.put("year",Integer.toString(year));
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
        dpd.show(getFragmentManager(), "DatePickerDialog");
    }

    private void sendNote(String cookieName, String cookieValue, String aID, String dayOfMonth, String monthOfYear, String year, String text) {
        new sendNoteToIW().execute(cookieName, cookieValue, aID, dayOfMonth, monthOfYear, year, text);
    }

    class sendNoteToIW extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String iwres = new String();
            Map<String,String> iwCookies = null;
            Elements csrftoken = null;
            String securityTokenKey = null;
            String securityTokenValue = null;

            try {
                Connection.Response iwRes = Jsoup.connect("http://agora.xtec.cat/escolapuigcerver/intranet/index.php?module=IWagendas&type=user&func=nova")
                        .userAgent("jsoup")
                        .cookie(params[0],params[1])
                        .method(Connection.Method.GET)
                        .execute();
                Document addNote = iwRes.parse();
                iwCookies = iwRes.cookies();
                csrftoken = addNote.select("[name=csrftoken]");
            }
            catch(IOException e) {
                e.printStackTrace();
            }

            for (Element token : csrftoken) {
                securityTokenKey = token.attr("name");
                securityTokenValue = token.attr("value");
            }

            try {
                Connection.Response iwresp = Jsoup.connect("http://agora.xtec.cat/escolapuigcerver/intranet/index.php?module=IWagendas&type=user&func=crear")
                        .data(securityTokenKey, securityTokenValue)
                        .data("daid", params[2])
                        .data("odaid", params[2])
                        .data("tasca", "0")
                        .data("gcalendar", "0")
                        .data("diatriat", params[3])
                        .data("mestriat", params[4])
                        .data("anytriat", params[5])
                        .data("horatriada", "00")
                        .data("minuttriat", "00")
                        .data("totdia", "1")
                        .data("oculta", "0")
                        .data("protegida", "0")
                        .data("c1", params[6])
                        .data("c2", "")
                        .data("repes", "0")
                        .data("repesdies", "")
                        .data("diatriatrep", "")
                        .data("mestriatrep", "")
                        .data("anytriatrep", "")
                        .cookies(iwCookies)
                        .cookie(params[0], params[1])
                        .userAgent("jsoup")
                        .followRedirects(true)
                        .method(Connection.Method.POST).execute();
                iwres = iwresp.parse().text();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return iwres;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.contains("creat")) {
                Snackbar.make(findViewById(R.id.fab), R.string.add_success, Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(findViewById(R.id.fab), R.string.add_fail, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void selectAgenda(boolean hasMap, final Map<String,String> agendes) {
        final SharedPreferences auth = AddToIWActivity.this.getSharedPreferences("auth", Context.MODE_PRIVATE);
        final String iwURL = "http://agora.xtec.cat/escolapuigcerver/intranet/index.php?module=IWAgendas&any=" + Calendar.getInstance().get(Calendar.YEAR) + "&llistat=1";
        final String cookieName = "ZKSID242";
        final String cookieValue = auth.getString("iwcookie",null);
            if (hasMap) {
                final CharSequence[] items = agendes.keySet().toArray(new CharSequence[agendes.size()]);
                new AlertDialog.Builder(AddToIWActivity.this)
                        .setTitle(R.string.choose_agenda_add)
                        .setSingleChoiceItems(items, 1, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int item) {
                            }
                        })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ListView lw = ((AlertDialog)dialog).getListView();
                                Object chosenAgenda = lw.getAdapter().getItem(lw.getCheckedItemPosition());
                                for (String s : agendes.keySet()) {
                                    if (s == chosenAgenda) {
                                        data.put("aID",agendes.get(s));
                                        Button mSetAgendaButton = (Button) findViewById(R.id.agenda);
                                        mSetAgendaButton.setText(s);
                                    }
                                }
                            }
                        })
                        .show();
            } else {
                new getAgendaList().execute(iwURL,cookieName,cookieValue);
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
                agendes.put(agtitle,id);
            }
            selectAgenda(true, agendes);
        }
    }
}
