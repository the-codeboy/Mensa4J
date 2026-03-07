package com.github.codeboy.api;

import com.github.codeboy.OpenMensa;
import com.github.codeboy.Util;
import com.github.codeboy.cache.MensaCacheManager;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReykjavikMensa implements Mensa {
    
    private static final String API_URL = "https://prod-198.westeurope.logic.azure.com/workflows/cc7c4c7157b14d5ba688859712303172/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=cRM1huMwILXk-jf6xybnCcTRpnSxjKY53jFwwUGLx14";
    private static final int REYKJAVIK_MENSA_ID = 999999;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    /**
     * Inner class representing the JSON structure from Reykjavik University API
     */
    private static class ReykjavikMenuEntry {
        @SerializedName("Title")
        private String title;
        
        @SerializedName("Date")
        private String date;
        
        @SerializedName("VeganMenu")
        private String veganMenu;
        
        @SerializedName("SoupOfTheDay")
        private String soupOfTheDay;
        
        @SerializedName("LongDay")
        private String longDay;
    }
    
    public ReykjavikMensa() {
    }
    
    private MensaCacheManager getCacheManager() {
        return OpenMensa.getInstance().getCacheManager();
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
    public List<Meal> getMeals(String dateString) {
        return getMeals(dateString, false);
    }
    
    @Override
    public List<Meal> getMeals(String dateString, boolean bypassCache) {
        // Check cache if not bypassing
        if (!bypassCache) {
            List<Meal> cachedMeals = getCacheManager().getCachedMeals(REYKJAVIK_MENSA_ID, dateString);
            if (cachedMeals != null) {
                return cachedMeals;
            }
        }
        
        // Fetch from network
        try {
            Type type = new TypeToken<List<ReykjavikMenuEntry>>() {}.getType();
            List<ReykjavikMenuEntry> entries = Util.getObject(API_URL, type);
            
            // Filter entries for the requested date
            List<Meal> meals = new ArrayList<>();
            for (ReykjavikMenuEntry entry : entries) {
                if (entry.date != null && entry.date.equals(dateString)) {
                    // Add main meal
                    if (entry.title != null && !entry.title.trim().isEmpty()) {
                        meals.add(createMeal(entry.title, "Main Dish", extractNotes(entry.title)));
                    }
                    
                    // Add vegan option
                    if (entry.veganMenu != null && !entry.veganMenu.trim().isEmpty()) {
                        List<String> veganNotes = extractNotes(entry.veganMenu);
                        veganNotes.add("Vegan");
                        meals.add(createMeal(cleanMealName(entry.veganMenu), "Vegan Option", veganNotes));
                    }
                    
                    // Add soup
                    if (entry.soupOfTheDay != null && !entry.soupOfTheDay.trim().isEmpty()) {
                        meals.add(createMeal(cleanMealName(entry.soupOfTheDay), "Soup of the Day", extractNotes(entry.soupOfTheDay)));
                    }
                    
                    break; // Only one entry per date
                }
            }
            
            // Cache the fetched meals
            getCacheManager().cacheMeals(REYKJAVIK_MENSA_ID, dateString, meals);
            
            return meals;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
    
    /**
     * Extract notes (allergen codes and dietary information) from meal name
     */
    private List<String> extractNotes(String mealText) {
        List<String> notes = new ArrayList<>();
        
        if (mealText == null) {
            return notes;
        }
        
        // Look for markers like (V), (K), etc. in parentheses
        if (mealText.contains("(V)")) {
            notes.add("Vegan");
        }
        if (mealText.contains("(K)")) {
            notes.add("Chicken");
        }
        
        return notes;
    }
    
    /**
     * Clean meal name by removing markers and excessive whitespace
     */
    private String cleanMealName(String mealText) {
        if (mealText == null) {
            return "";
        }
        
        // Remove markers in parentheses at the end
        String cleaned = mealText.replaceAll("\\s*\\([A-Z]\\)\\s*$", "");
        
        // Trim and collapse multiple whitespaces/tabs
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    /**
     * Create a Meal object with no price information
     */
    private Meal createMeal(String name, String category, List<String> notes) {
        // Reykjavik University doesn't provide price information in their API
        Prices prices = new Prices(null, null, null, null);
        return new Meal(cleanMealName(name), category, notes, prices);
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
    public boolean isOpen(String dateString) {
        // Check cache
        Boolean cachedOpeningTimes = getCacheManager().getCachedOpeningTimes(REYKJAVIK_MENSA_ID, dateString);
        if (cachedOpeningTimes != null) {
            return cachedOpeningTimes;
        }
        
        // Reykjavik mensa is open if there are meals for that date
        try {
            Date date = DATE_FORMAT.parse(dateString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            // Open Monday to Friday (Calendar.MONDAY = 2, Calendar.FRIDAY = 6)
            boolean isOpen = (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY);
            
            // Cache the result
            getCacheManager().cacheOpeningTimes(REYKJAVIK_MENSA_ID, dateString, isOpen);
            
            return isOpen;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    @Override
    public int getId() {
        return REYKJAVIK_MENSA_ID;
    }
    
    @Override
    public String getName() {
        return "Reykjavik University Cafeteria";
    }
    
    @Override
    public String getCity() {
        return "Reykjavik";
    }
    
    @Override
    public String getAddress() {
        return "Menntavegur 1, 101 Reykjavik, Iceland";
    }
    
    @Override
    public List<Double> getCoordinates() {
        // Coordinates for Reykjavik University
        return Arrays.asList(64.1293, -21.9033);
    }
    
    @Override
    public boolean hasOpeningHours() {
        return false;
    }
    
    @Override
    public float getOpeningTime(Date date) {
        return 0; // Unknown
    }
    
    @Override
    public float getClosingTime(Date date) {
        return 0; // Unknown
    }
}
