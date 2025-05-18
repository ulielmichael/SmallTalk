package mu.smalltalk.Services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.Message;
import mu.smalltalk.entitis.User;
import mu.smalltalk.repositoriy.MessageRepository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private GroupService groupService;
    
    @Autowired
    private EncryptionService encryptionService;
    
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
    }
    
    /**
     * Get all formatted messages for a group
     * 
     * @param groupId The group ID
     * @return List of formatted message strings
     */
    public List<String> getGroupMessages(String groupId) {
        List<Message> messages = messageRepository.findByChatId(groupId);
        
        return messages.stream()
                .map(message -> new String(message.getTextContent(), StandardCharsets.UTF_8))
                .collect(Collectors.toList());
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
            return messageRepository.save(message);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}