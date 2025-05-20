package mu.smalltalk.Services;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mu.smalltalk.entitis.Message;
import mu.smalltalk.entitis.User;
import mu.smalltalk.Repositories.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    /**
     * Save a new message to the database
     * 
     * @param senderID Sender's ID (email)
     * @param receiverID Receiver's ID (can be a group ID)
     * @param textContent Encrypted text content
     * @param mediaContent Optional media content bytes
     * @param mediaContentType Media content type (IMAGE/SOUND)
     * @param chatId Chat or group ID
     * @return The saved message
     */
    public Message saveMessage(String senderID, String receiverID, byte[] textContent, 
                               byte[] mediaContent, String mediaContentType, String chatId) {
        
        Message message = new Message(senderID, receiverID, textContent, mediaContent, mediaContentType, chatId);
        return messageRepository.save(message);
    }
    
    /**
     * Save a text-only message
     * 
     * @param senderID Sender's ID
     * @param receiverID Receiver's ID
     * @param textContent Encrypted text content
     * @param chatId Chat or group ID
     * @return The saved message
     */
    public Message saveTextMessage(String senderID, String receiverID, byte[] textContent, String chatId) {
        return saveMessage(senderID, receiverID, textContent, null, null, chatId);
    }
    
    /**
     * Get all messages for a specific chat/group
     * 
     * @param chatId The chat or group ID
     * @return List of messages for the chat
     */
    public List<Message> getMessagesByChatId(String chatId) {
        return messageRepository.findByChatId(chatId);
    }
    
    /**
     * Get all chats/groups that a user has participated in
     * 
     * @param userId The user's ID (email)
     * @return List of unique chat IDs
     */
    public List<String> getUserChatIds(String userId) {
        // Find all messages where the user is either sender or receiver
        List<Message> sentMessages = messageRepository.findBySenderID(userId);
        List<Message> receivedMessages = messageRepository.findByReceiverID(userId);
        
        // Combine the lists and extract unique chat IDs
        return sentMessages.stream()
                .map(Message::getChatId)
                .distinct()
                .collect(Collectors.toList());
    }
}