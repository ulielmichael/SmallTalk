package mu.smalltalk.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.Message;
import mu.smalltalk.entitis.User;
import mu.smalltalk.Repositories.MessageRepository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ChatService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private GroupService groupService;
    
    @Autowired
    private EncryptionService encryptionService;
    
    // Use a thread-safe map to track online users
    private static final Map<String, Long> onlineUsers = new ConcurrentHashMap<>();
    // Consider a user online if they've had activity in the last 5 minutes
    private static final long ONLINE_TIMEOUT_MS = 5 * 60 * 1000;
    
    // Cache for message history to improve performance
    private static final Map<String, List<String>> messageCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> cacheLastUpdated = new ConcurrentHashMap<>();
    // Cache timeout - 30 seconds
    private static final long CACHE_TIMEOUT_MS = 30 * 1000;
    
    /**
     * Add a formatted message for a group to the database
     * 
     * @param formattedMessage The formatted HTML message
     * @param groupId The group ID
     * @param senderId The sender's ID (email)
     */
    public void addGroupMessage(String formattedMessage, String groupId, String senderId) {
        // Convert the formatted message to bytes
        byte[] messageBytes = formattedMessage.getBytes(StandardCharsets.UTF_8);
        
        // Save to database
        Message message = new Message(senderId, groupId, messageBytes, null, null, groupId);
        messageRepository.save(message);
        
        // Invalidate cache for this group
        messageCache.remove(groupId);
    }
    
    /**
     * Get all formatted messages for a group
     * 
     * @param groupId The group ID
     * @return List of formatted message strings
     */
    public List<String> getGroupMessages(String groupId) {
        // Check if we have a recent cache
        Long lastUpdated = cacheLastUpdated.get(groupId);
        long now = System.currentTimeMillis();
        
        if (lastUpdated != null && (now - lastUpdated) < CACHE_TIMEOUT_MS && messageCache.containsKey(groupId)) {
            return new ArrayList<>(messageCache.get(groupId));
        }
        
        // If no recent cache, get from database
        List<Message> messages = messageRepository.findByChatId(groupId);
        
        List<String> formattedMessages = messages.stream()
                .filter(message -> message.getTextContent() != null)
                .map(message -> new String(message.getTextContent(), StandardCharsets.UTF_8))
                .collect(Collectors.toList());
        
        // Update cache
        messageCache.put(groupId, formattedMessages);
        cacheLastUpdated.put(groupId, now);
        
        return formattedMessages;
    }
    
    /**
     * Load messages in a background thread to improve UI responsiveness
     * 
     * @param groupId The group ID
     * @return CompletableFuture containing the list of messages
     */
    public CompletableFuture<List<String>> loadMessagesAsync(String groupId) {
        return CompletableFuture.supplyAsync(() -> getGroupMessages(groupId));
    }
    
    /**
     * Get all groups that a user has participated in (sent or received messages)
     * 
     * @param userEmail The user's email
     * @return List of groups
     */
    public List<Group> getUserActiveGroups(String userEmail) {
        // Get all groups the user is a member of
        List<Group> userGroups = groupService.getUserGroups(userEmail);
        
        // Find which groups have messages
        List<Group> activeGroups = new ArrayList<>();
        
        for (Group group : userGroups) {
            List<Message> messages = messageRepository.findByChatId(group.getId());
            if (!messages.isEmpty()) {
                activeGroups.add(group);
            }
        }
        
        return activeGroups;
    }
    
    /**
     * Add an encrypted message to the database
     * 
     * @param message The plain text message
     * @param groupId The group ID
     * @param senderId The sender's email
     * @return The message object that was saved
     */
    public Message addEncryptedMessage(String message, String groupId, String senderId) {
        try {
            // Encrypt the message
            byte[] encryptedBytes = encryptionService.encrypt(message);
            
            // Create and save the message
            Message messageObj = new Message(senderId, groupId, encryptedBytes, null, null, groupId);
            
            // Invalidate cache for this group
            messageCache.remove(groupId);
            
            return messageRepository.save(messageObj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Add media content to the database
     * 
     * @param mediaBytes The media file bytes
     * @param mediaType The media type (IMAGE/SOUND)
     * @param groupId The group ID
     * @param senderId The sender's email
     * @return The message object that was saved
     */
    public Message addMediaMessage(String mediaBytes, String mediaType, String groupId, String senderId) {
        try {
            // Encrypt the media content (optional)
            byte[] encryptedMedia = encryptionService.encrypt(mediaBytes);
            
            // Create and save the message (with null text content)
            Message message = new Message(senderId, groupId, null, encryptedMedia, mediaType, groupId);
            
            // Invalidate cache for this group
            messageCache.remove(groupId);
            
            return messageRepository.save(message);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Update user activity timestamp
     * @param userEmail The user's email
     */
    public void updateUserActivity(String userEmail) {
        if (userEmail != null && !userEmail.isEmpty()) {
            onlineUsers.put(userEmail, System.currentTimeMillis());
        }
    }

    /**
     * Check if a user is currently online
     * @param userEmail The user's email
     * @return true if the user is online, false otherwise
     */
    public boolean isUserOnline(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            return false;
        }
        
        Long lastActivity = onlineUsers.get(userEmail);
        if (lastActivity == null) {
            return false;
        }
        
        // Check if the user has been active within the timeout period
        return (System.currentTimeMillis() - lastActivity) < ONLINE_TIMEOUT_MS;
    }
    
    /**
     * Get all currently online users
     * @return Map of user emails to last activity timestamps
     */
    public Map<String, Long> getOnlineUsers() {
        long cutoffTime = System.currentTimeMillis() - ONLINE_TIMEOUT_MS;
        
        // Clean up old entries and return current ones
        Map<String, Long> activeUsers = new HashMap<>();
        onlineUsers.forEach((email, timestamp) -> {
            if (timestamp >= cutoffTime) {
                activeUsers.put(email, timestamp);
            } else {
                onlineUsers.remove(email);
            }
        });
        
        return activeUsers;
    }
    
    /**
     * Remove a user from the online users map
     * @param userEmail The user's email
     */
    public void removeUser(String userEmail) {
        if (userEmail != null && !userEmail.isEmpty()) {
            onlineUsers.remove(userEmail);
        }
    }
    
    /**
     * Clear the message cache for specific group
     * @param groupId The group ID
     */
    public void clearCache(String groupId) {
        messageCache.remove(groupId);
        cacheLastUpdated.remove(groupId);
    }
    
    
    /**
     * Clear all message caches
     */
    public void clearAllCaches() {
        messageCache.clear();
        cacheLastUpdated.clear();
    }   
}