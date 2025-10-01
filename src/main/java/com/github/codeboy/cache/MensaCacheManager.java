package com.github.codeboy.cache;

import com.github.codeboy.api.Meal;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * A cache manager specifically designed for Mensa meal data and opening times.
 * This class coordinates between memory and disk cache to provide fast access
 * and persistent storage of meal data.
 */
public class MensaCacheManager {
    
    private static final String MEALS_KEY_PREFIX = "meals_";
    private static final String OPENING_TIMES_KEY_PREFIX = "opening_";
    private static final long MEAL_CACHE_EXPIRATION_HOURS = 24 * 30; // keep cached meals for one month
    private static final long OPENING_TIMES_CACHE_EXPIRATION_HOURS = 24 * 7; // update opening times once a week
    
    private final PersistentCache cache;
    
    /**
     * Create a new MensaCacheManager with the default cache implementation.
     */
    public MensaCacheManager() {
        this(new FilePersistentCache());
    }
    
    /**
     * Create a new MensaCacheManager with a specific cache implementation.
     * 
     * @param cache The cache implementation to use
     */
    public MensaCacheManager(PersistentCache cache) {
        this.cache = cache;
    }
    
    /**
     * Cache meals for a specific mensa and date.
     * 
     * @param mensaId The ID of the mensa
     * @param date The date string (YYYY-MM-DD format)
     * @param meals The list of meals to cache
     */
    public void cacheMeals(int mensaId, String date, List<Meal> meals) {
        String key = MEALS_KEY_PREFIX + mensaId + "_" + date;
        long expirationTime = System.currentTimeMillis() + (MEAL_CACHE_EXPIRATION_HOURS * 60 * 60 * 1000);
        cache.put(key, meals, expirationTime);
    }
    
    /**
     * Retrieve cached meals for a specific mensa and date.
     * 
     * @param mensaId The ID of the mensa
     * @param date The date string (YYYY-MM-DD format)
     * @return The cached list of meals, or null if not found or expired
     */
    public List<Meal> getCachedMeals(int mensaId, String date) {
        String key = MEALS_KEY_PREFIX + mensaId + "_" + date;
        
        try {
            // Use TypeToken to properly handle generic List<Meal> deserialization
            Type listType = new TypeToken<List<Meal>>(){}.getType();
            return cache.get(key, listType);
        } catch (Exception e) {
            System.err.println("Warning: Failed to retrieve cached meals: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if meals are cached for a specific mensa and date.
     * 
     * @param mensaId The ID of the mensa
     * @param date The date string (YYYY-MM-DD format)
     * @return true if meals are cached and not expired, false otherwise
     */
    public boolean hasCachedMeals(int mensaId, String date) {
        String key = MEALS_KEY_PREFIX + mensaId + "_" + date;
        return cache.contains(key);
    }
    
    /**
     * Cache opening times for a specific mensa and date.
     * 
     * @param mensaId The ID of the mensa
     * @param date The date string (YYYY-MM-DD format)
     * @param isOpen Whether the mensa is open on this date
     */
    public void cacheOpeningTimes(int mensaId, String date, boolean isOpen) {
        String key = OPENING_TIMES_KEY_PREFIX + mensaId + "_" + date;
        long expirationTime = System.currentTimeMillis() + (OPENING_TIMES_CACHE_EXPIRATION_HOURS * 60 * 60 * 1000);
        cache.put(key, isOpen, expirationTime);
    }
    
    /**
     * Retrieve cached opening times for a specific mensa and date.
     * 
     * @param mensaId The ID of the mensa
     * @param date The date string (YYYY-MM-DD format)
     * @return The cached opening status, or null if not found or expired
     */
    public Boolean getCachedOpeningTimes(int mensaId, String date) {
        String key = OPENING_TIMES_KEY_PREFIX + mensaId + "_" + date;
        return cache.get(key, Boolean.class);
    }
    
    /**
     * Check if opening times are cached for a specific mensa and date.
     * 
     * @param mensaId The ID of the mensa
     * @param date The date string (YYYY-MM-DD format)
     * @return true if opening times are cached and not expired, false otherwise
     */
    public boolean hasCachedOpeningTimes(int mensaId, String date) {
        String key = OPENING_TIMES_KEY_PREFIX + mensaId + "_" + date;
        return cache.contains(key);
    }
    
    /**
     * Remove cached meals for a specific mensa and date.
     * 
     * @param mensaId The ID of the mensa
     * @param date The date string (YYYY-MM-DD format)
     * @return true if the entry was removed, false if it didn't exist
     */
    public boolean removeCachedMeals(int mensaId, String date) {
        String key = MEALS_KEY_PREFIX + mensaId + "_" + date;
        return cache.remove(key);
    }
    
    /**
     * Remove cached opening times for a specific mensa and date.
     * 
     * @param mensaId The ID of the mensa
     * @param date The date string (YYYY-MM-DD format)
     * @return true if the entry was removed, false if it didn't exist
     */
    public boolean removeCachedOpeningTimes(int mensaId, String date) {
        String key = OPENING_TIMES_KEY_PREFIX + mensaId + "_" + date;
        return cache.remove(key);
    }
    
    /**
     * Clear all cached data for a specific mensa.
     * 
     * @param mensaId The ID of the mensa
     */
    public void clearMensaCache(int mensaId) {
        String mealsPrefix = MEALS_KEY_PREFIX + mensaId + "_";
        String openingPrefix = OPENING_TIMES_KEY_PREFIX + mensaId + "_";
        
        List<String> allKeys = cache.getAllKeys();
        for (String key : allKeys) {
            if (key.startsWith(mealsPrefix) || key.startsWith(openingPrefix)) {
                cache.remove(key);
            }
        }
    }
    
    /**
     * Clear all expired cache entries.
     * 
     * @return The number of expired entries that were removed
     */
    public int clearExpired() {
        return cache.clearExpired();
    }
    
    /**
     * Clear all cache entries.
     */
    public void clearAll() {
        cache.clearAll();
    }
    
    /**
     * Get the number of cached entries.
     * 
     * @return The number of cache entries
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Get the underlying cache implementation.
     * 
     * @return The cache implementation
     */
    public PersistentCache getCache() {
        return cache;
    }
}