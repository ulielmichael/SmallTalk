package mu.smalltalk.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;

import mu.smalltalk.entitis.Message;
import mu.smalltalk.entitis.User;
import mu.smalltalk.Repositories.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class ChatService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private MessageService messageService;
    
    // In-memory cache for frequently accessed messages
    private final Map<String, List<String>> messageCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> userLastActivity = new ConcurrentHashMap<>();
    
    // Cache timeout in minutes
    private static final int CACHE_TIMEOUT_MINUTES = 5;
    private static final int BATCH_SIZE = 50; // Load messages in batches
    
    /**
     * Add a message to a group - with cache invalidation
     */
    @CacheEvict(value = "groupMessages", key = "#groupId")
    public void addGroupMessage(String message, String groupId, String senderId) {
        try {
            // Save to database asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    // Convert message to bytes for encryption storage if needed
                    byte[] messageBytes = message.getBytes();
                    messageService.saveTextMessage(senderId, groupId, messageBytes, groupId);
                } catch (Exception e) {
                    System.err.println("Error saving message to database: " + e.getMessage());
                }
            });
            
            // Update in-memory cache immediately for faster UI updates
            messageCache.computeIfAbsent(groupId, k -> new ArrayList<>()).add(message);
            cacheTimestamps.put(groupId, LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("Error adding group message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get group messages with intelligent caching
     */
    public List<String> getGroupMessages(String groupId) {
        if (groupId == null) {
            return new ArrayList<>();
        }
        
        // Check if cache is valid and recent
        if (isCacheValid(groupId)) {
            return new ArrayList<>(messageCache.get(groupId));
        }
        
        // Cache is invalid or doesn't exist, reload from database
        return loadAndCacheMessages(groupId);
    }
    
    /**
     * Asynchronously load messages for better performance
     */
    @Async
    public CompletableFuture<List<String>> loadMessagesAsync(String groupId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (groupId == null) {
                    return new ArrayList<>();
                }
                
                // Check cache first
                if (isCacheValid(groupId)) {
                    return new ArrayList<>(messageCache.get(groupId));
                }
                
                // Load from database with pagination for better performance
                List<String> messages = loadMessagesFromDatabase(groupId);
                
                // Update cache
                messageCache.put(groupId, new ArrayList<>(messages));
                cacheTimestamps.put(groupId, LocalDateTime.now());
                
                return messages;
                
            } catch (Exception e) {
                System.err.println("Error loading messages asynchronously: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Load messages from database with optimized query
     */
    private List<String> loadMessagesFromDatabase(String groupId) {
        try {
            // Create pageable with sorting by time descending
            Pageable pageable = PageRequest.of(0, BATCH_SIZE, Sort.by(Sort.Direction.DESC, "time"));
            
            // Get messages from database - limit to recent messages for performance
            List<Message> dbMessages = messageRepository.findByChatIdOrderByTimeDesc(groupId, pageable);
            
            // Convert to strings (decrypt if needed)
            return dbMessages.stream()
                .map(this::convertMessageToString)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("Error loading messages from database: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Convert database message to display string
     */
    private String convertMessageToString(Message message) {
        try {
            // If message has text content (encrypted), decrypt it
            if (message.getTextContent() != null) {
                String decryptedText = new String(message.getTextContent());
                return decryptedText;
            }
            
            // Handle media messages
            if (message.getMediaContent() != null) {
                String mediaType = message.getMediaContentType();
                return String.format("[%s] Media message: %s", 
                    message.getTime().toString(), mediaType);
            }
            
            return "Empty message";
            
        } catch (Exception e) {
            System.err.println("Error converting message: " + e.getMessage());
            return "Error loading message";
        }
    }
    
    /**
     * Load and cache messages synchronously
     */
    private List<String> loadAndCacheMessages(String groupId) {
        try {
            List<String> messages = loadMessagesFromDatabase(groupId);
            
            // Update cache
            messageCache.put(groupId, new ArrayList<>(messages));
            cacheTimestamps.put(groupId, LocalDateTime.now());
            
            return messages;
            
        } catch (Exception e) {
            System.err.println("Error loading and caching messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if cache is valid and recent
     */
    private boolean isCacheValid(String groupId) {
        if (!messageCache.containsKey(groupId) || !cacheTimestamps.containsKey(groupId)) {
            return false;
        }
        
        LocalDateTime cacheTime = cacheTimestamps.get(groupId);
        LocalDateTime now = LocalDateTime.now();
        
        return cacheTime.plusMinutes(CACHE_TIMEOUT_MINUTES).isAfter(now);
    }
    
    /**
     * Clear cache for a specific group
     */
    @CacheEvict(value = "groupMessages", key = "#groupId")
    public void clearGroupCache(String groupId) {
        messageCache.remove(groupId);
        cacheTimestamps.remove(groupId);
    }
    
    /**
     * Clear all caches (useful for memory management)
     */
    @CacheEvict(value = "groupMessages", allEntries = true)
    public void clearAllCaches() {
        messageCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * Update user activity status
     */
    public void updateUserActivity(String userEmail) {
        if (userEmail != null) {
            userLastActivity.put(userEmail, LocalDateTime.now());
        }
    }
    
    /**
     * Check if user is online (active in last 2 minutes)
     */
    public boolean isUserOnline(String userEmail) {
        if (userEmail == null || !userLastActivity.containsKey(userEmail)) {
            return false;
        }
        
        LocalDateTime lastActivity = userLastActivity.get(userEmail);
        LocalDateTime now = LocalDateTime.now();
        
        return lastActivity.plusMinutes(2).isAfter(now);
    }
    
    /**
     * Get all online users for a group
     */
    public List<String> getOnlineUsersInGroup(String groupId, List<String> groupMembers) {
        return groupMembers.stream()
            .filter(this::isUserOnline)
            .collect(Collectors.toList());
    }
    
    /**
     * Preload messages for multiple groups (useful for user login)
     */
    @Async
    public CompletableFuture<Void> preloadGroupMessages(List<String> groupIds) {
        return CompletableFuture.runAsync(() -> {
            for (String groupId : groupIds) {
                if (!isCacheValid(groupId)) {
                    loadAndCacheMessages(groupId);
                }
            }
        });
    }
    
    /**
     * Get message count for a group (for pagination)
     */
    public long getMessageCount(String groupId) {
        try {
            return messageRepository.countByChatId(groupId);
        } catch (Exception e) {
            System.err.println("Error getting message count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Load older messages (pagination support)
     */
    public List<String> loadOlderMessages(String groupId, int offset, int limit) {
        try {
            // Create pageable for pagination
            int page = offset / limit;
            Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "time"));
            
            List<Message> olderMessages = messageRepository.findByChatIdWithPagination(groupId, pageable);
            
            return olderMessages.stream()
                .map(this::convertMessageToString)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("Error loading older messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}