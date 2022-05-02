package com.github.codeboy;

import com.github.codeboy.api.Mensa;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class OpenMensa {

    private static final OpenMensa mensa = new OpenMensa();
    private final HashMap<Integer, Mensa> canteens = new HashMap<>();
    private String baseUrl = "http://openmensa.org/api/v2";

    private OpenMensa() {
    }

    public static OpenMensa getInstance() {
        return mensa;
    }

    private void loadCanteens() {
        List<Mensa> mensas;
        int page = 1;
        do {
            try {
                mensas = getCanteens(page++);
                for (Mensa mensa : mensas) {
                    mensa.init();
                    canteens.put(mensa.getId(), mensa);
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        } while (!mensas.isEmpty());
    }

    private List<Mensa> getCanteens(int page) throws Exception {
        Type type = new TypeToken<List<Mensa>>() {
        }.getType();
        return Util.getObject(baseUrl + "/canteens/?page=" + page, type);
    }

    public void reloadCanteens() {
        canteens.clear();
        loadCanteens();
    }

    public Mensa getMensa(int id) {
        if (canteens.containsKey(id))
            return canteens.get(id);
        try {
            return Util.getObject(baseUrl + "/canteens/" + id, Mensa.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Optional<Mensa> getOptionalMensa(int id) {
        return Optional.of(getMensa(id));
    }

    public Collection<Mensa> getAllCanteens() {
        return canteens.values();
    }

    public List<Mensa> searchMensa(String searchString) {
        if (searchString == null || searchString.length() == 0) {
            return Collections.emptyList();
        }
        String finalSearchString = searchString.toLowerCase();
        return getAllCanteens().stream()
                .filter(mensa -> mensa.getName().toLowerCase().contains(finalSearchString))
                .collect(Collectors.toList());
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
