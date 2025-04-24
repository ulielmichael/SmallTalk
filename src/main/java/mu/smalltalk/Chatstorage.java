package mu.smalltalk;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Chatstorage {
    // For backward compatibility - keep the global message list
    private static final List<String> messages = new ArrayList<>();
    
    // New conversation-based storage
    private static final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

    /**
     * Add a message to the global message list (for backward compatibility)
     */
    public static synchronized void addMessage(String message) {
        messages.add(message);
    }

    /**
     * Get all messages (for backward compatibility)
     */
    public static synchronized List<String> getMessages() {
        return new ArrayList<>(messages);
    }
    
    /**
     * Add a message to a specific conversation
     * @param conversationId The ID of the conversation
     * @param message The message to add
     */
    public static synchronized void addMessageToConversation(String conversationId, Message message) {
        conversations.computeIfAbsent(conversationId, k -> new CopyOnWriteArrayList<>()).add(message);
        
        // Also add a string representation to the global message list for backward compatibility
        String formattedMessage = createFormattedMessageString(message);
        addMessage(formattedMessage);
    }
    
    /**
     * Get all messages for a specific conversation
     * @param conversationId The ID of the conversation
     * @return A list of messages in the conversation
     */
    public static synchronized List<Message> getConversationMessages(String conversationId) {
        return new ArrayList<>(conversations.getOrDefault(conversationId, new ArrayList<>()));
    }
    
    /**
     * Get all conversations
     * @return A map of conversation IDs to lists of messages
     */
    public static synchronized Map<String, List<Message>> getAllConversations() {
        Map<String, List<Message>> result = new HashMap<>();
        for (Map.Entry<String, List<Message>> entry : conversations.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Create a formatted string representation of a message
     */
    private static String createFormattedMessageString(Message message) {
        String formattedTime = message.getTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String textContent = message.getTextContent() != null ? new String(message.getTextContent()) : "";
        
        return "[" + formattedTime + "] " + message.getSenderID() + " -> " + message.getReceiverID() + ": " + textContent;
    }
}