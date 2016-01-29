package com.henry.iwagenda;

import java.util.Date;

public class Event {
    private final String text;
    private final Date date;
    private final boolean isAllDay;
    private final Agenda agenda;

    public Event(String text, Date date, boolean isAllDay, Agenda agenda) {
        this.text = text;
        this.date = date;
        this.isAllDay = isAllDay;
        this.agenda = agenda;
    }

    public String getText() {
        return text;
    }

    public Date getDate() {
        return date;
    }

    public boolean isAllDay() {
        return isAllDay;
    }

    public Agenda getAgenda() {
        return agenda;
    }
}
