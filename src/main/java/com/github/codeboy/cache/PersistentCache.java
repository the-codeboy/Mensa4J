package com.github.codeboy.cache;

import java.util.List;

/**
 * A generic cache interface for persisting data with expiration support.
 * This interface is designed to work across platforms including Android
 * without requiring any platform-specific dependencies.
 */
public interface PersistentCache {
    
    /**
     * Store an object in the cache with a specific key and expiration time.
     * 
     * @param key The unique key to identify the cached object
     * @param value The object to cache
     * @param expirationTimeMillis The time in milliseconds when this cache entry should expire
     */
    <T> void put(String key, T value, long expirationTimeMillis);
    
    /**
     * Store an object in the cache with a specific key and default expiration (24 hours).
     * 
     * @param key The unique key to identify the cached object
     * @param value The object to cache
     */
    <T> void put(String key, T value);
    
    /**
     * Retrieve an object from the cache.
     * 
     * @param key The key used to store the object
     * @param clazz The class type of the object
     * @return The cached object, or null if not found or expired
     */
    <T> T get(String key, Class<T> clazz);
    
    /**
     * Retrieve an object from the cache using a Type token for generic types.
     * 
     * @param key The key used to store the object
     * @param type The Type token for the object (supports generics like List<Meal>)
     * @return The cached object, or null if not found or expired
     */
    <T> T get(String key, java.lang.reflect.Type type);
    
    /**
     * Check if a cache entry exists and is not expired.
     * 
     * @param key The key to check
     * @return true if the key exists and is not expired, false otherwise
     */
    boolean contains(String key);
    
    /**
     * Remove a specific cache entry.
     * 
     * @param key The key to remove
     * @return true if the key was removed, false if it didn't exist
     */
    boolean remove(String key);
    
    /**
     * Clear all expired cache entries.
     * 
     * @return The number of entries that were removed
     */
    int clearExpired();
    
    /**
     * Clear all cache entries.
     */
    void clearAll();
    
    /**
     * Get the list of all cache keys (including expired ones).
     * 
     * @return List of all cache keys
     */
    List<String> getAllKeys();
    
    /**
     * Get the size of the cache (number of entries).
     * 
     * @return The number of cache entries
     */
    int size();
}