package com.github.codeboy.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A file-based cache implementation that persists data to JSON files.
 * This implementation is platform-agnostic and works on Android without requiring Android APIs.
 * 
 * The cache stores data in a directory structure with individual JSON files for each cache entry.
 * Each file contains both the data and metadata (expiration time, creation time).
 */
public class FilePersistentCache implements PersistentCache {
    
    private static final String CACHE_FILE_EXTENSION = ".cache.json";
    private static final long DEFAULT_EXPIRATION_HOURS = 24;
    private static final long DEFAULT_EXPIRATION_MILLIS = DEFAULT_EXPIRATION_HOURS * 60 * 60 * 1000;
    
    private final Path cacheDirectory;
    private final Gson gson;
    private final Map<String, CacheEntry> memoryCache;
    private boolean diskCache = true;
    
    /**
     * Create a new FilePersistentCache with the default cache directory.
     * The cache directory will be created in the user's home directory under ".mensa4j/cache".
     */
    public FilePersistentCache() {
        this(getDefaultCacheDirectory());
    }
    
    /**
     * Create a new FilePersistentCache with a specific cache directory.
     * 
     * @param cacheDirectory The directory where cache files will be stored
     */
    public FilePersistentCache(String cacheDirectory) {
        this.cacheDirectory = Paths.get(cacheDirectory);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.memoryCache = new ConcurrentHashMap<>();
        
        // Create cache directory if it doesn't exist
        createCacheDirectory();
        
        // Load existing cache entries from disk
        if(diskCache)
            loadExistingCache();
    }
    
    /**
     * Get the default cache directory path.
     * On Android, this should be in the app's internal storage.
     * On desktop systems, this will be in the user's home directory.
     */
    private static String getDefaultCacheDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            // Fallback for systems where user.home is not available (some Android environments)
            userHome = System.getProperty("user.dir", ".");
        }
        return userHome + File.separator + ".mensa4j" + File.separator + "cache";
    }
    
    private void createCacheDirectory() {
        try {
            if (!Files.exists(cacheDirectory)) {
                Files.createDirectories(cacheDirectory);
            }
        } catch (IOException e) {
            System.err.println("Failed to create cache directory: " + cacheDirectory + " will only cache to ram. " + e.getMessage());
            diskCache = false;
        }
    }
    
    private void loadExistingCache() {
        try {
            if (!Files.exists(cacheDirectory)) {
                return;
            }
            
            Files.list(cacheDirectory)
                    .filter(path -> path.toString().endsWith(CACHE_FILE_EXTENSION))
                    .forEach(this::loadCacheEntryFromFile);
                    
        } catch (IOException e) {
            System.err.println("Warning: Failed to load existing cache entries: " + e.getMessage());
        }
    }
    
    private void loadCacheEntryFromFile(Path filePath) {
        try {
            String content = new String(Files.readAllBytes(filePath));
            Type type = new TypeToken<CacheEntry>(){}.getType();
            CacheEntry entry = gson.fromJson(content, type);
            
            if (entry != null && !entry.isExpired()) {
                String key = extractKeyFromFileName(filePath.getFileName().toString());
                memoryCache.put(key, entry);
            } else if (entry != null && entry.isExpired()) {
                // Remove expired file
                Files.deleteIfExists(filePath);
            }
        } catch (IOException | JsonParseException e) {
            System.err.println("Warning: Failed to load cache entry from " + filePath + ": " + e.getMessage());
            // Delete corrupted file
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException deleteEx) {
                // Ignore deletion errors
            }
        }
    }
    
    private String extractKeyFromFileName(String fileName) {
        return fileName.substring(0, fileName.length() - CACHE_FILE_EXTENSION.length());
    }
    
    private String sanitizeKeyForFileName(String key) {
        // Replace characters that are not safe for file names
        return key.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    private Path getCacheFilePath(String key) {
        String sanitizedKey = sanitizeKeyForFileName(key);
        return cacheDirectory.resolve(sanitizedKey + CACHE_FILE_EXTENSION);
    }
    
    @Override
    public <T> void put(String key, T value, long expirationTimeMillis) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }
        
        CacheEntry entry = new CacheEntry(value, expirationTimeMillis);
        memoryCache.put(key, entry);
        
        // Persist to disk
        try {
            Path filePath = getCacheFilePath(key);
            String json = gson.toJson(entry);
            Files.write(filePath, json.getBytes());
        } catch (IOException e) {
            System.err.println("Warning: Failed to persist cache entry to disk: " + e.getMessage());
            // Continue operation even if disk write fails - we still have it in memory
        }
    }
    
    @Override
    public <T> void put(String key, T value) {
        long expirationTime = System.currentTimeMillis() + DEFAULT_EXPIRATION_MILLIS;
        put(key, value, expirationTime);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        if (key == null) {
            return null;
        }
        
        CacheEntry entry = memoryCache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            remove(key);
            return null;
        }
        
        try {
            Object data = entry.getData();
            if (data == null) {
                return null;
            }
            
            // If the data is already of the correct type, return it directly
            if (clazz.isInstance(data)) {
                return (T) data;
            }
            
            // Otherwise, try to deserialize it using Gson
            String json = gson.toJson(data);
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            System.err.println("Warning: Failed to deserialize cache entry for key '" + key + "': " + e.getMessage());
            remove(key);
            return null;
        }
    }
    
    @Override
    public <T> T get(String key, java.lang.reflect.Type type) {
        if (key == null) {
            return null;
        }
        
        CacheEntry entry = memoryCache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            remove(key);
            return null;
        }
        
        try {
            Object data = entry.getData();
            if (data == null) {
                return null;
            }
            
            // For generic types, we need to use Gson to properly deserialize
            String json = gson.toJson(data);
            return gson.fromJson(json, type);
        } catch (Exception e) {
            System.err.println("Warning: Failed to deserialize cache entry for key '" + key + "' with type '" + type + "': " + e.getMessage());
            remove(key);
            return null;
        }
    }
    
    @Override
    public boolean contains(String key) {
        if (key == null) {
            return false;
        }
        
        CacheEntry entry = memoryCache.get(key);
        if (entry == null) {
            return false;
        }
        
        if (entry.isExpired()) {
            remove(key);
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean remove(String key) {
        if (key == null) {
            return false;
        }
        
        CacheEntry removed = memoryCache.remove(key);
        
        // Remove from disk
        try {
            Path filePath = getCacheFilePath(key);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Warning: Failed to remove cache file: " + e.getMessage());
        }
        
        return removed != null;
    }
    
    @Override
    public int clearExpired() {
        int removedCount = 0;
        Iterator<Map.Entry<String, CacheEntry>> iterator = memoryCache.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                
                // Remove from disk
                try {
                    Path filePath = getCacheFilePath(entry.getKey());
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    System.err.println("Warning: Failed to remove expired cache file: " + e.getMessage());
                }
                
                removedCount++;
            }
        }
        
        return removedCount;
    }
    
    @Override
    public void clearAll() {
        // Clear memory cache
        Set<String> keys = new HashSet<>(memoryCache.keySet());
        memoryCache.clear();
        
        // Remove all cache files
        for (String key : keys) {
            try {
                Path filePath = getCacheFilePath(key);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("Warning: Failed to remove cache file: " + e.getMessage());
            }
        }
    }
    
    @Override
    public List<String> getAllKeys() {
        return new ArrayList<>(memoryCache.keySet());
    }
    
    @Override
    public int size() {
        return memoryCache.size();
    }
    
    /**
     * Get the cache directory path.
     * 
     * @return The path to the cache directory
     */
    public Path getCacheDirectory() {
        return cacheDirectory;
    }
    
    /**
     * Perform maintenance operations like clearing expired entries.
     * This method should be called periodically to keep the cache clean.
     * 
     * @return The number of expired entries that were removed
     */
    public int performMaintenance() {
        return clearExpired();
    }
}