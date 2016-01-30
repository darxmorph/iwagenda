package com.henry.iwagenda;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class iwAPI {
    protected static final String userAgent = "com.henry.iwagenda/Android (1.4/release) jsoup/1.8.3";
    protected static final String iwURL = "http://agora.xtec.cat/escolapuigcerver/intranet/index.php?module=IWAgendas&llistat=1";

    @SuppressLint("SimpleDateFormat")
    protected static class iwDateFormats {
        protected static final DateFormat day = new SimpleDateFormat("dd");
        protected static final DateFormat month = new SimpleDateFormat("MM");
        protected static final DateFormat year = new SimpleDateFormat("yy");
        protected static final DateFormat fullYear = new SimpleDateFormat("yyyy");
        protected static final DateFormat hour = new SimpleDateFormat("HH");
        protected static final DateFormat minute = new SimpleDateFormat("mm");
        protected static final DateFormat date = new SimpleDateFormat("dd/MM/yyyy");
        protected static final DateFormat time = new SimpleDateFormat("HH:mm");
        protected static final DateFormat full = new SimpleDateFormat("dd/MM/yyyyHH:mm");
    }

    /**
     * Gets dates that contains events
     *
     * @param  agendas  Set that contains agendas to look for events
     * @param  cookies  Session cookies (obtained after login)
     * @param  months   Number of months in the future to look for events
     * @return          Dates that contain events (Set)
     */
    public Set<Date> getEventDateSet(Set<Agenda> agendas, Map<String, String> cookies, int months) {
        Set<Date> homeworkDateSet = new HashSet<>();
        Elements iwe = new Elements();

        try {
            iwe = new parseIWagenda(agendas, months, cookies).execute().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Element element : iwe) {
            try {
                Date dr = iwDateFormats.date.parse(element.text());
                homeworkDateSet.add(dr);
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
        }

        return homeworkDateSet;
    }

    /**
     * Gets a set of events in the date specified
     *
     * @param  agendas  Set that contains the list of agenda IDs to check
     * @param  date     Date to get the events from
     * @param  cookies  Session cookies (obtained after login)
     * @return          Events for date (Set)
     */
    public Set<Event> getEventsForDate(Set<Agenda> agendas, Date date, Map<String, String> cookies) {
        try {
            return new parseIWday(date,agendas,cookies).execute().get();
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    /**
     * Gets Agendas available for user
     *
     * @param  cookies  Session cookies (obtained after login)
     * @return          Agendas (Set)
     */
    public Set<Agenda> getAgendas(Map<String,String> cookies) {
        Set<Agenda> agendaSet = new HashSet<>();

        Elements el = new Elements();
        try {
            el = new getAgendas(cookies).execute().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Element agenda : el) {
            String agtitle = agenda.attr("title");
            String agurl = agenda.attr("href");
            String id = agurl.substring(agurl.lastIndexOf("&daid=") + 6);
            agtitle = agtitle.trim().replaceAll(" +", " ");
            agendaSet.add(new Agenda(id,agtitle));
        }

        return agendaSet;
    }

    /**
     * Publishes an event to IW
     *
     * @param  event    Event to publish (also contains agenda)
     * @param  cookies  Session cookies (obtained after login)
     * @return          true if successful, otherwise false
     */
    public boolean sendEventToIW(Event event, Map<String,String> cookies) {
        try {
            return new sendEventToIW(event, cookies).execute().get();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private class parseIWagenda extends AsyncTask<String, Void, Elements> {
        private final Set<Agenda> agendas;
        private final int months;
        private final Map<String, String> cookies;

        parseIWagenda(Set<Agenda> agendas, int months, Map<String, String> cookies) {
            this.agendas = agendas;
            this.months = months;
            this.cookies = cookies;
        }

        @Override
        protected Elements doInBackground(String... params) {
            Elements iwe = new Elements();
            Calendar present = Calendar.getInstance();

            Calendar lastDate = Calendar.getInstance();
            lastDate.add(Calendar.MONTH,months);

            try {
                for (present.get(Calendar.MONTH); present.before(lastDate); present.add(Calendar.MONTH, 1)) {
                    for (Agenda a : agendas) {
                        Document iw = Jsoup.connect(iwAPI.iwURL + "&any=" + present.get(Calendar.YEAR) + "&mes=" + (present.get(Calendar.MONTH) + 1) + "&daid=" + a.getId()).cookies(cookies).userAgent(iwAPI.userAgent).get();
                        Elements ev = iw.getElementsByAttributeValueStarting("id", "note_");
                        iwe.addAll(ev);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return iwe;
        }
    }

    private class getAgendas extends AsyncTask<String, Void, Elements> {
        private final Map<String,String> cookies;

        getAgendas(Map<String,String> cookies) {
            this.cookies = cookies;
        }

        @Override
        protected Elements doInBackground(String... params) {
            Elements el = new Elements();

            try {
                Document iw = Jsoup.connect(iwAPI.iwURL)
                        .userAgent(iwAPI.userAgent)
                        .cookies(cookies)
                        .get();
                el = iw.select("[href^=index.php?module=IWAgendas&mes=" + iwDateFormats.month.format(new Date()) + "&any=" + iwDateFormats.fullYear.format(new Date()) + "&daid=]");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return el;
        }
    }

    private class parseIWday extends AsyncTask<String, Void, Set<Event>> {
        final Date date;
        final Set<Agenda> agendas;
        final Map<String,String> cookies;

        parseIWday(Date date, Set<Agenda> agendas, Map<String,String> cookies) {
            this.date = date;
            this.agendas = agendas;
            this.cookies = cookies;
        }

        @Override
        protected Set<Event> doInBackground(String... params) {
            Set<Event> eventsForDate = new HashSet<>();

            try {
                for (Agenda a : agendas) {
                    Document iw = Jsoup.connect(iwAPI.iwURL + "&any=" + iwDateFormats.year.format(date) + "&mes=" + iwDateFormats.month.format(date) + "&dia=" + iwDateFormats.day.format(date) + "&daid=" + a.getId()).cookies(cookies).userAgent(iwAPI.userAgent).get();
                    Elements events = iw.getElementsByAttributeValueStarting("id", "note_");

                    if (events.hasText()) {
                        for (Element element : events) {
                            // Event constructors
                            final String eventText;
                            final Date eventDate;
                            final boolean eventIsAllDay;
                            final Agenda agenda;

                            Date d = date;
                            agenda = a;

                            String text = element.text();
                            if (text.contains("Tot el dia")) {
                                text = text.replace("Tot el dia", "");
                                eventIsAllDay = true;
                            } else {
                                eventIsAllDay = false;
                                try {
                                    d = iwDateFormats.full.parse(iwDateFormats.date.format(date) + iwDateFormats.time.format(iwDateFormats.time.parse(element.text())));
                                    text = text.replace(iwDateFormats.time.format(d), "");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            eventDate = d;
                            text = text.replace("---", "");

                            // Weird IW bug
                            if (!a.getId().equals("0"))
                                text = text.substring(1);

                            text = text.trim();
                            eventText = text;

                            if (!a.getId().equals("0")) {
                                eventsForDate.add(new Event(eventText, eventDate, eventIsAllDay, agenda));
                            } else {
                                // If got this item from Personal, make sure it didn't come from subscribed Agendas
                                if (!text.contains("Altres")) {
                                    eventsForDate.add(new Event(eventText, eventDate, eventIsAllDay, agenda));
                                }
                            }
                        }
                    }
                }
            }

            catch(Throwable t) {
                t.printStackTrace();
            }

            return eventsForDate;
        }
    }

    class sendEventToIW extends AsyncTask<String, Void, Boolean> {
        final Event event;
        final Agenda agenda;
        final Map<String,String> cookies;

        sendEventToIW(Event event, Map<String,String> cookies) {
            this.event = event;
            this.agenda = event.getAgenda();
            this.cookies = cookies;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String iwres = "";
            Map<String,String> iwCookies = new HashMap<>();
            Elements csrftoken = new Elements();
            String securityTokenKey = "";
            String securityTokenValue = "";

            try {
                Connection.Response iwRes = Jsoup.connect("http://agora.xtec.cat/escolapuigcerver/intranet/index.php?module=IWagendas&type=user&func=nova")
                        .userAgent(iwAPI.userAgent)
                        .cookies(cookies)
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
                        .data("daid", agenda.getId())
                        .data("odaid", agenda.getId())
                        .data("tasca", "0")
                        .data("gcalendar", "0")
                        .data("diatriat", iwDateFormats.day.format(event.getDate()))
                        .data("mestriat", iwDateFormats.month.format(event.getDate()))
                        .data("anytriat", iwDateFormats.year.format(event.getDate()))
                        .data("horatriada", iwDateFormats.hour.format(event.getDate()))
                        .data("minuttriat", iwDateFormats.minute.format(event.getDate()))
                        .data("totdia", (event.isAllDay()) ? "1" : "0")
                        .data("oculta", "0")
                        .data("protegida", "0")
                        .data("c1", event.getText())
                        .data("c2", "")
                        .data("repes", "0")
                        .data("repesdies", "")
                        .data("diatriatrep", "")
                        .data("mestriatrep", "")
                        .data("anytriatrep", "")
                        .cookies(iwCookies)
                        .cookies(cookies)
                        .userAgent(iwAPI.userAgent)
                        .followRedirects(true)
                        .method(Connection.Method.POST).execute();
                iwres = iwresp.parse().text();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return iwres.contains("creat");
        }
    }
}
