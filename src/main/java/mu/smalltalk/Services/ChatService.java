// package mu.smalltalk.Services;

// import mu.smalltalk.Aes256;
// import mu.smalltalk.Message;
// import mu.smalltalk.Chatstorage;
// import mu.smalltalk.repositoriy.MessageRepository;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.ExecutionException;

// @Service
// public class ChatService {
    
//     @Autowired
//     private MessageRepository messageRepository;
    
//     @Autowired
//     private EncryptionService encryptionService;
    
//     /**
//      * Save a text message with encryption
//      * 
//      * @param senderId The sender's ID
//      * @param receiverId The receiver's ID
//      * @param plainText The plain text message to encrypt
//      * @param chatId The chat ID
//      * @return The saved message entity
//      * @throws Exception If encryption or saving fails
//      */
//     public Message saveTextMessage(String senderId, String receiverId, String plainText, String chatId) throws Exception {
//         try {
//             // Encrypt the message text asynchronously
//             CompletableFuture<byte[]> encryptionFuture = encryptionService.encryptStringAsync(plainText);
            
//             // Wait for encryption to complete and get the encrypted data
//             byte[] encryptedContent = encryptionFuture.get();
            
//             // Create a new message with encrypted content
//             Message message = new Message(senderId, receiverId, encryptedContent, null, null, chatId);
            
//             // Save the message to MongoDB
//             Message savedMessage = messageRepository.save(message);
            
//             // Also store in the local storage for backward compatibility
//             Chatstorage.addMessageToConversation(chatId, savedMessage);
            
//             // Broadcast through the existing system (using plaintext for display)
//             String formattedMessage = createFormattedMessageString(savedMessage, plainText);
//             GlobalMessageBroadcaster.broadcast(formattedMessage);
            
//             return savedMessage;
//         } catch (InterruptedException | ExecutionException e) {
//             throw new Exception("Failed to encrypt and save message: " + e.getMessage(), e);
//         }
//     }
    
//     /**
//      * Save a message with media content (both text and media are encrypted)
//      * 
//      * @param senderId The sender's ID
//      * @param receiverId The receiver's ID
//      * @param plainText The plain text message to encrypt
//      * @param mediaContent The media content bytes to encrypt
//      * @param mediaContentType The media content type (IMAGE/SOUND)
//      * @param chatId The chat ID
//      * @return The saved message entity
//      * @throws Exception If encryption or saving fails
//      */
//     public Message saveMediaMessage(String senderId, String receiverId, String plainText, 
//                             byte[] mediaContent, String mediaContentType, String chatId) throws Exception {
//         try {
//             // Encrypt text content
//             CompletableFuture<byte[]> textEncryptionFuture = encryptionService.encryptStringAsync(plainText);
            
//             // Encrypt media content
//             CompletableFuture<byte[]> mediaEncryptionFuture = encryptionService.encryptAsync(mediaContent);
            
//             // Wait for both encryption processes to complete
//             byte[] encryptedText = textEncryptionFuture.get();
//             byte[] encryptedMedia = mediaEncryptionFuture.get();
            
//             // Create a new message with encrypted content
//             Message message = new Message(senderId, receiverId, encryptedText, encryptedMedia, mediaContentType, chatId);
            
//             // Save the message to MongoDB
//             Message savedMessage = messageRepository.save(message);
            
//             // Store in local storage for backward compatibility
//             Chatstorage.addMessageToConversation(chatId, savedMessage);
            
//             // Broadcast through the existing system (using plaintext for display)
//             String formattedMessage = createFormattedMessageString(savedMessage, plainText) + " [with " + mediaContentType + "]";
//             GlobalMessageBroadcaster.broadcast(formattedMessage);
            
//             return savedMessage;
//         } catch (InterruptedException | ExecutionException e) {
//             throw new Exception("Failed to encrypt and save media message: " + e.getMessage(), e);
//         }
//     }
    
//     /**
//      * Retrieve conversation messages and decrypt the content
//      * 
//      * @param chatId The chat ID
//      * @return List of messages with decrypted content
//      * @throws Exception If retrieval or decryption fails
//      */
//     public List<Message> getConversationMessages(String chatId) throws Exception {
//         List<Message> encryptedMessages = messageRepository.findByChatId(chatId);
//         return decryptMessages(encryptedMessages);
//     }
    
//     /**
//      * Retrieve messages between two users and decrypt the content
//      * 
//      * @param senderId The sender's ID
//      * @param receiverId The receiver's ID
//      * @return List of messages with decrypted content
//      * @throws Exception If retrieval or decryption fails
//      */
//     public List<Message> getMessagesBetweenUsers(String senderId, String receiverId) throws Exception {
//         List<Message> encryptedMessages = messageRepository.findBySenderIDAndReceiverID(senderId, receiverId);
//         return decryptMessages(encryptedMessages);
//     }
    
//     /**
//      * Retrieve messages for a user and decrypt the content
//      * 
//      * @param userId The user ID
//      * @return List of messages with decrypted content
//      * @throws Exception If retrieval or decryption fails
//      */
//     public List<Message> getMessagesForUser(String userId) throws Exception {
//         List<Message> encryptedMessages = messageRepository.findByReceiverID(userId);
//         return decryptMessages(encryptedMessages);
//     }
    
//     /**
//      * Decrypt a list of messages
//      * 
//      * @param encryptedMessages List of messages with encrypted content
//      * @return List of messages with decrypted content
//      * @throws Exception If decryption fails
//      */
//     private List<Message> decryptMessages(List<Message> encryptedMessages) throws Exception {
//         List<Message> decryptedMessages = new ArrayList<>();
//         List<CompletableFuture<Void>> decryptionFutures = new ArrayList<>();
        
//         for (Message encryptedMessage : encryptedMessages) {
//             Message decryptedMessage = new Message();
//             decryptedMessage.setSenderID(encryptedMessage.getSenderID());
//             decryptedMessage.setReceiverID(encryptedMessage.getReceiverID());
//             decryptedMessage.setChatId(encryptedMessage.getChatId());
//             decryptedMessage.setTime(encryptedMessage.getTime());
//             decryptedMessage.setMediaContentType(encryptedMessage.getMediaContentType());
            
//             // Add to result list immediately
//             decryptedMessages.add(decryptedMessage);
            
//             // Start the decryption of text content asynchronously
//             if (encryptedMessage.getTextContent() != null) {
//                 CompletableFuture<Void> textDecryptionFuture = encryptionService
//                     .decryptToStringAsync(encryptedMessage.getTextContent())
//                     .thenAccept(decryptedText -> 
//                         decryptedMessage.setTextContent(decryptedText.getBytes())
//                     );
                
//                 decryptionFutures.add(textDecryptionFuture);
//             }
            
//             // Start the decryption of media content asynchronously, if present
//             if (encryptedMessage.getMediaContent() != null) {
//                 CompletableFuture<Void> mediaDecryptionFuture = encryptionService
//                     .decryptAsync(encryptedMessage.getMediaContent())
//                     .thenAccept(decryptedMedia -> 
//                         decryptedMessage.setMediaContent(decryptedMedia)
//                     );
                
//                 decryptionFutures.add(mediaDecryptionFuture);
//             }
//         }
        
//         // Wait for all decryption operations to complete
//         CompletableFuture.allOf(decryptionFutures.toArray(new CompletableFuture[0])).get();
        
//         return decryptedMessages;
//     }
    
//     /**
//      * Retrieve and decrypt media content from a message
//      * 
//      * @param messageId The message ID
//      * @return Decrypted media bytes
//      * @throws Exception If retrieval or decryption fails
//      */
//     public byte[] getDecryptedMedia(String messageId) throws Exception {
//         Message message = messageRepository.findById(messageId)
//             .orElseThrow(() -> new Exception("Message not found"));
        
//         if (message.getMediaContent() == null) {
//             throw new Exception("No media content in this message");
//         }
        
//         try {
//             return encryptionService.decryptAsync(message.getMediaContent()).get();
//         } catch (InterruptedException | ExecutionException e) {
//             throw new Exception("Failed to decrypt media: " + e.getMessage(), e);
//         }
//     }
    
//     /**
//      * Save a message directly
//      * (Legacy method preserved for compatibility)
//      */
//     public Message saveMessage(Message message) {
//         // Save message to MongoDB
//         Message savedMessage = messageRepository.save(message);
        
//         // Also broadcast the message through the existing system
//         String formattedMessage = createFormattedMessageString(savedMessage);
//         GlobalMessageBroadcaster.broadcast(formattedMessage);
        
//         return savedMessage;
//     }
    
//     /**
//      * Create a formatted string representation of a message for broadcasting
//      * (Legacy method preserved for compatibility)
//      */
//     private String createFormattedMessageString(Message message) {
//         String formattedTime = message.getTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//         String textContent = message.getTextContent() != null ? new String(message.getTextContent()) : "";
                
//         return "[" + formattedTime + "] " + message.getSenderID() + " -> " + message.getReceiverID() + ": " + textContent;
//     }
    
//     /**
//      * Create a formatted string representation of a message with provided plaintext content
//      * Used when we already have the plaintext before encryption
//      */
//     private String createFormattedMessageString(Message message, String plainTextContent) {
//         String formattedTime = message.getTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
//         return "[" + formattedTime + "] " + message.getSenderID() + " -> " + message.getReceiverID() + ": " + plainTextContent;
//     }
// }