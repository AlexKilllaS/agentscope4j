package com.agentscope.memory;

import com.agentscope.module.StateModule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Base class for long-term memory management in AgentScope4J.
 * Provides persistent storage and retrieval of information across sessions.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public abstract class LongTermMemoryBase extends StateModule {
    
    /**
     * Store information in long-term memory.
     * 
     * @param key The key to store the information under
     * @param value The information to store
     * @param metadata Optional metadata associated with the information
     */
    public abstract void store(String key, Object value, Map<String, Object> metadata);
    
    /**
     * Store information with default metadata.
     * 
     * @param key The key to store the information under
     * @param value The information to store
     */
    public void store(String key, Object value) {
        store(key, value, null);
    }
    
    /**
     * Retrieve information from long-term memory by key.
     * 
     * @param key The key to retrieve information for
     * @return The stored information, or null if not found
     */
    public abstract Object retrieve(String key);
    
    /**
     * Retrieve information with metadata.
     * 
     * @param key The key to retrieve information for
     * @return Map containing 'value' and 'metadata' keys, or null if not found
     */
    public abstract Map<String, Object> retrieveWithMetadata(String key);
    
    /**
     * Search for information based on query criteria.
     * 
     * @param query The search query
     * @param limit Maximum number of results to return
     * @return List of search results
     */
    public abstract List<Map<String, Object>> search(String query, int limit);
    
    /**
     * Search with default limit.
     * 
     * @param query The search query
     * @return List of search results (default limit applied)
     */
    public List<Map<String, Object>> search(String query) {
        return search(query, 10); // Default limit
    }
    
    /**
     * Advanced search with multiple criteria.
     * 
     * @param criteria Search criteria as key-value pairs
     * @param limit Maximum number of results to return
     * @return List of search results
     */
    public abstract List<Map<String, Object>> searchByCriteria(
        Map<String, Object> criteria, int limit);
    
    /**
     * Get similar information based on semantic similarity.
     * 
     * @param reference The reference information to find similar items for
     * @param limit Maximum number of similar items to return
     * @return List of similar items with similarity scores
     */
    public abstract List<Map<String, Object>> findSimilar(Object reference, int limit);
    
    /**
     * Update existing information in long-term memory.
     * 
     * @param key The key of the information to update
     * @param value The new value
     * @param metadata The new metadata
     * @return true if the update was successful, false if key not found
     */
    public abstract boolean update(String key, Object value, Map<String, Object> metadata);
    
    /**
     * Update with default metadata preservation.
     * 
     * @param key The key of the information to update
     * @param value The new value
     * @return true if the update was successful, false if key not found
     */
    public boolean update(String key, Object value) {
        Map<String, Object> existing = retrieveWithMetadata(key);
        Map<String, Object> metadata = existing != null ? 
            (Map<String, Object>) existing.get("metadata") : null;
        return update(key, value, metadata);
    }
    
    /**
     * Delete information from long-term memory.
     * 
     * @param key The key of the information to delete
     * @return true if the deletion was successful, false if key not found
     */
    public abstract boolean delete(String key);
    
    /**
     * Check if a key exists in long-term memory.
     * 
     * @param key The key to check
     * @return true if the key exists
     */
    public abstract boolean exists(String key);
    
    /**
     * Get all keys stored in long-term memory.
     * 
     * @return List of all keys
     */
    public abstract List<String> getAllKeys();
    
    /**
     * Get the total number of items in long-term memory.
     * 
     * @return Total item count
     */
    public abstract long getItemCount();
    
    /**
     * Clear all information from long-term memory.
     * Use with caution as this operation is irreversible.
     */
    public abstract void clearAll();
    
    /**
     * Create a backup of the long-term memory.
     * 
     * @return Backup data that can be used for restoration
     */
    public abstract Map<String, Object> createBackup();
    
    /**
     * Restore long-term memory from a backup.
     * 
     * @param backup The backup data to restore from
     * @return true if restoration was successful
     */
    public abstract boolean restoreFromBackup(Map<String, Object> backup);
    
    /**
     * Get statistics about the long-term memory usage.
     * 
     * @return Map containing various statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "total_items", getItemCount(),
            "memory_type", getClass().getSimpleName(),
            "supports_search", supportsSearch(),
            "supports_similarity", supportsSimilaritySearch(),
            "supports_backup", supportsBackup()
        );
    }
    
    /**
     * Check if this implementation supports text search.
     * 
     * @return true if search is supported
     */
    public boolean supportsSearch() {
        return true; // Default implementation
    }
    
    /**
     * Check if this implementation supports similarity search.
     * 
     * @return true if similarity search is supported
     */
    public boolean supportsSimilaritySearch() {
        return false; // Default implementation
    }
    
    /**
     * Check if this implementation supports backup/restore.
     * 
     * @return true if backup/restore is supported
     */
    public boolean supportsBackup() {
        return true; // Default implementation
    }
    
    /**
     * Optimize the long-term memory storage.
     * This might include operations like compaction, indexing, etc.
     */
    public void optimize() {
        // Default implementation does nothing
        // Subclasses can override for specific optimizations
    }
    
    /**
     * Get memory health information.
     * 
     * @return Map containing health metrics
     */
    public Map<String, Object> getHealthInfo() {
        return Map.of(
            "status", "healthy",
            "item_count", getItemCount(),
            "last_accessed", java.time.LocalDateTime.now().toString()
        );
    }
    
    /**
     * Validate the integrity of the long-term memory.
     * 
     * @return true if memory is in a consistent state
     */
    public boolean validateIntegrity() {
        try {
            // Basic validation - check if we can get item count
            getItemCount();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get configuration information for this memory instance.
     * 
     * @return Map containing configuration details
     */
    public abstract Map<String, Object> getConfiguration();
    
    /**
     * Update configuration for this memory instance.
     * 
     * @param config New configuration parameters
     * @return true if configuration was updated successfully
     */
    public abstract boolean updateConfiguration(Map<String, Object> config);
    
    @Override
    public String toString() {
        return String.format("%s{itemCount=%d, supportsSearch=%s, supportsSimilarity=%s}",
                           getClass().getSimpleName(),
                           getItemCount(),
                           supportsSearch(),
                           supportsSimilaritySearch());
    }
}