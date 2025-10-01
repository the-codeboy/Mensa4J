package com.github.codeboy;

import com.github.codeboy.api.Mensa;
import com.github.codeboy.api.MensaImpl;
import com.github.codeboy.api.RWTHMensa;
import com.github.codeboy.cache.MensaCacheManager;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class OpenMensa {

    private static final OpenMensa mensa = new OpenMensa();
    private final HashMap<Integer, Mensa> canteens = new HashMap<>();
    private String baseUrl = "https://openmensa.org/api/v2";
    private MensaCacheManager cacheManager;

    private OpenMensa() {
        cacheManager = new MensaCacheManager();
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
                    if (mensa instanceof MensaImpl)
                        ((MensaImpl) mensa).init();
                    canteens.put(mensa.getId(), mensa);
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        } while (!mensas.isEmpty());
        RWTHMensa.injectRWTHCanteens(canteens);
    }

    private List<Mensa> getCanteens(int page) throws Exception {
        Type type = new TypeToken<List<MensaImpl>>() {
        }.getType();
        return Util.getObject(baseUrl + "/canteens/?page=" + page, type);
    }

    public void reloadCanteens() {
        canteens.clear();
        loadCanteens();
    }

    public void reloadRWTHCanteens() {
        RWTHMensa.injectRWTHCanteens(canteens);
    }

    public Mensa getMensa(int id) {
        if (canteens.containsKey(id))
            return canteens.get(id);
        try {
            return Util.getObject(baseUrl + "/canteens/" + id, MensaImpl.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addMensa(Mensa mensa) {
        canteens.put(mensa.getId(), mensa);
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

    public MensaCacheManager getCacheManager(){
        return cacheManager;
    }

    public void setCacheManager(MensaCacheManager cacheManager){
        this.cacheManager = cacheManager;
    }
}
