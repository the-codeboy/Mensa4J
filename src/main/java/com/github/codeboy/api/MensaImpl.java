package com.github.codeboy.api;

import com.github.codeboy.OpenMensa;
import com.github.codeboy.Util;
import com.github.codeboy.cache.MensaCacheManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MensaImpl implements Mensa {
    private final int id;
    private final String name, city, address;
    private final List<Double> coordinates;

    private final MensaCacheManager cacheManager;

    public MensaImpl(int id, String name, String city, String address, List<Double> coordinates) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.address = address;
        this.coordinates = coordinates;
        this.cacheManager = new MensaCacheManager();
    }

    public void init() {
        // Cache manager handles initialization
    }


    @Override
    public List<Meal> getMeals(Date date) {
        return getMeals(Util.dateToString(date));
    }

    @Override
    public List<Meal> getMeals(Date date, boolean bypassCache) {
        return getMeals(Util.dateToString(date), bypassCache);
    }

    @Override
    public List<Meal> getMeals(String date) {
        return getMeals(date, false);
    }

    @Override
    public List<Meal> getMeals(String date, boolean bypassCache) {
        // Check cache if not bypassing
        if (!bypassCache) {
            List<Meal> cachedMeals = cacheManager.getCachedMeals(id, date);
            if (cachedMeals != null) {
                return cachedMeals;
            }
        }

        // Fetch from network
        try {
            Type type = new TypeToken<List<Meal>>() {
            }.getType();
            List<Meal> fetchedMeals = Util.getObject(OpenMensa.getInstance().getBaseUrl() + "/canteens/" + id + "/days/" + date + "/meals/", type);
            
            // Cache the fetched meals
            cacheManager.cacheMeals(id, date, fetchedMeals);
            
            return fetchedMeals;
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
        // Check cache
        Boolean cachedOpeningTimes = cacheManager.getCachedOpeningTimes(id, date);
        if (cachedOpeningTimes != null) {
            return cachedOpeningTimes;
        }

        // Fetch from network
        try {
            JsonElement e = JsonParser.parseString(Util.readUrl("https://openmensa.org/api/v2/canteens/" + id + "/days/" + date + "/"));
            boolean closed = e.getAsJsonObject().get("closed").getAsBoolean();
            boolean isOpen = !closed;
            
            // Cache the result
            cacheManager.cacheOpeningTimes(id, date, isOpen);
            
            return isOpen;
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

    @Override
    public boolean hasOpeningHours() {
        return false;
    }
}
