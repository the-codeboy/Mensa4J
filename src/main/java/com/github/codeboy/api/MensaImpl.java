package com.github.codeboy.api;

import com.github.codeboy.OpenMensa;
import com.github.codeboy.Util;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MensaImpl implements Mensa {
    private final int id;
    private final String name, city, address;
    private final List<Double> coordinates;

    private HashMap<String, List<Meal>> meals = new HashMap<>();
    private HashMap<String, Boolean> openingTimes = new HashMap<>();

    public MensaImpl(int id, String name, String city, String address, List<Double> coordinates) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.address = address;
        this.coordinates = coordinates;
    }

    public void init() {
        if (meals == null)
            meals = new HashMap<>();
        if (openingTimes == null)
            openingTimes = new HashMap<>();
    }


    @Override
    public List<Meal> getMeals() {
        return getMeals(new Date());
    }

    @Override
    public List<Meal> getMeals(Date date) {
        return getMeals(Util.dateToString(date));
    }

    @Override
    public List<Meal> getMeals(String date) {
        if (meals.containsKey(date)) {
            return meals.get(date);
        }

        try {
            Type type = new TypeToken<List<Meal>>() {
            }.getType();
            List<Meal> meals = Util.getObject(OpenMensa.getInstance().getBaseUrl() + "/canteens/" + id + "/days/" + date + "/meals/", type);
            this.meals.put(date, meals);
            return meals;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isOpen() {
        return isOpen(new Date());
    }

    @Override
    public boolean isOpen(Date date) {
        return isOpen(Util.dateToString(date));
    }

    @Override
    public boolean isOpen(String date) {
        if (openingTimes.containsKey(date)) {
            return openingTimes.get(date);
        }

        try {
            JsonElement e = JsonParser.parseString(Util.readUrl("https://openmensa.org/api/v2/canteens/" + id + "/days/" + date + "/"));
            boolean closed = e.getAsJsonObject().get("closed").getAsBoolean();
            this.openingTimes.put(date, !closed);
            return !closed;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public List<Double> getCoordinates() {
        return coordinates;
    }
}
