package com.henry.iwagenda;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Calendar;
import java.util.Map;

public class AddToIWActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        Button mSetDateButton = (Button) findViewById(R.id.date);
        mSetDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initDateSelect();
            }
        });
    }
    private void initDateSelect() {
        DatePickerDialog.OnDateSetListener odsl = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                Button mSetDateButton = (Button) findViewById(R.id.date);
                mSetDateButton.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" +year);
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
        new sendNoteToIW().execute(cookieName,cookieValue,aID,dayOfMonth,monthOfYear,year,text);
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
                        .data("odaid", "0")
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
            new AlertDialog.Builder(AddToIWActivity.this)
                    .setTitle("IW")
                    .setMessage(result)
                    .setIcon(R.drawable.ic_pencil)
                    .show();
        }
    }

}
