package com.github.codeboy.cache;

/**
 * A wrapper class for cache entries that includes metadata such as expiration time.
 * This class is used internally by cache implementations to track cache entry lifecycle.
 */
public class CacheEntry {
    private final Object data;
    private final long expirationTime;
    private final long creationTime;
    
    public CacheEntry(Object data, long expirationTime) {
        this.data = data;
        this.expirationTime = expirationTime;
        this.creationTime = System.currentTimeMillis();
    }
    
    public Object getData() {
        return data;
    }
    
    public long getExpirationTime() {
        return expirationTime;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Check if this cache entry has expired.
     * 
     * @return true if the entry has expired, false otherwise
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
    
    /**
     * Get the remaining time until expiration in milliseconds.
     * 
     * @return milliseconds until expiration, or 0 if already expired
     */
    public long getTimeUntilExpiration() {
        long remaining = expirationTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}