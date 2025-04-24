package mu.smalltalk.Services;


import mu.smalltalk.Message;
import mu.smalltalk.repositoriy.MessageRepository;
import mu.smalltalk.Services.GlobalMessageBroadcaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {
    
    @Autowired
    private mu.smalltalk.repositoriy.MessageRepository messageRepository;

    public Message saveMessage(Message message) {
        // Save message to MongoDB
        Message savedMessage = messageRepository.save(message);
        
        // Also broadcast the message through the existing system
        String formattedMessage = createFormattedMessageString(savedMessage);
        GlobalMessageBroadcaster.broadcast(formattedMessage);
        
        return savedMessage;
    }
    
    public List<Message> getConversationMessages(String chatId) {
        return messageRepository.findByChatId(chatId);
    }
    
    public List<Message> getMessagesBetweenUsers(String senderId, String receiverId) {
        return messageRepository.findBySenderIDAndReceiverID(senderId, receiverId);
    }
    
    public List<Message> getMessagesForUser(String userId) {
        return messageRepository.findByReceiverID(userId);
    }
    
    private String createFormattedMessageString(Message message) {
        String formattedTime = message.getTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String textContent = message.getTextContent() != null ? new String(message.getTextContent()) : "";
                
        return "[" + formattedTime + "] " + message.getSenderID() + " -> " + message.getReceiverID() + ": " + textContent;
    }
}