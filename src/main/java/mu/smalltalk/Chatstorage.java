package mu.smalltalk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory storage for chat messages
 */
public class Chatstorage {
    private static final List<String> messages = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, List<String>> groupMessages = new ConcurrentHashMap<>();
    
    private static final int MAX_MESSAGES = 100; // Maximum number of messages to store per group
    
    /**
     * Add a message to the global chat storage
     * @param message The message to add
     */
    public static void addMessage(String message) {
        synchronized (messages) {
            messages.add(message);
            
            // Keep only the last MAX_MESSAGES messages
            if (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
        }
    }
    
    /**
     * Add a message to a specific group's chat storage
     * @param message The message to add
     * @param groupId The ID of the group
     */
    public static void addGroupMessage(String message, String groupId) {
        if (groupId == null) {
            addMessage(message); // Fall back to global messages
            return;
        }
        
        groupMessages.computeIfAbsent(groupId, k -> Collections.synchronizedList(new ArrayList<>()));
        
        List<String> groupMessageList = groupMessages.get(groupId);
        synchronized (groupMessageList) {
            groupMessageList.add(message);
            
            // Keep only the last MAX_MESSAGES messages
            if (groupMessageList.size() > MAX_MESSAGES) {
                groupMessageList.remove(0);
            }
        }
    }
    
    /**
     * Get all messages from the global chat storage
     * @return List of messages
     */
    public static List<String> getAllMessages() {
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }
    
    /**
     * Get all messages for a specific group
     * @param groupId The ID of the group
     * @return List of messages for the group, or empty list if no messages
     */
    public static List<String> getGroupMessages(String groupId) {
        if (groupId == null) {
            return getAllMessages(); // Fall back to global messages
        }
        
        List<String> groupMessageList = groupMessages.get(groupId);
        if (groupMessageList == null) {
            return new ArrayList<>();
        }
        
        synchronized (groupMessageList) {
            return new ArrayList<>(groupMessageList);
        }
    }
    
    /**
     * Clear all messages from all storages
     */
    public static void clearAllMessages() {
        synchronized (messages) {
            messages.clear();
        }
        
        groupMessages.clear();
    }
    
    /**
     * Clear all messages for a specific group
     * @param groupId The ID of the group
     */
    public static void clearGroupMessages(String groupId) {
        if (groupId == null) {
            return;
        }
        
        List<String> groupMessageList = groupMessages.get(groupId);
        if (groupMessageList != null) {
            synchronized (groupMessageList) {
                groupMessageList.clear();
            }
        }
    }

    public static void addMessageToGroup(String message, String groupId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addMessageToGroup'");
    }
}