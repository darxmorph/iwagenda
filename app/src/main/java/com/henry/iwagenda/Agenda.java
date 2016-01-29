package com.henry.iwagenda;

public class Agenda {
    private final String id;
    private final String name;

    public Agenda(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
