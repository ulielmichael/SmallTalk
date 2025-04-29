// package mu.smalltalk.Controller;

// import mu.smalltalk.Message;
// import mu.smalltalk.Services.ChatService;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile;

// import java.util.List;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/chat")
// public class ChatController {

//     @Autowired
//     private ChatService chatService;
    
//     /**
//      * Send an encrypted text message
//      */
//     @PostMapping("/messages/text")
//     public ResponseEntity<?> sendTextMessage(@RequestBody Map<String, String> messageData) {
//         try {
//             String senderId = messageData.get("senderId");
//             String receiverId = messageData.get("receiverId");
//             String text = messageData.get("text");
//             String chatId = messageData.get("chatId");
            
//             if (senderId == null || receiverId == null || text == null || chatId == null) {
//                 return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
//             }
            
//             Message message = chatService.saveTextMessage(senderId, receiverId, text, chatId);
//             return ResponseEntity.ok(Map.of(
//                 "id", message.getChatId(),
//                 "timestamp", message.getTime().toString(),
//                 "status", "Message sent successfully"
//             ));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                 .body(Map.of("error", "Failed to send message: " + e.getMessage()));
//         }
//     }
    
//     /**
//      * Send an encrypted message with media (image or sound)
//      */
//     @PostMapping(value = "/messages/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//     public ResponseEntity<?> sendMediaMessage(
//             @RequestParam("senderId") String senderId,
//             @RequestParam("receiverId") String receiverId,
//             @RequestParam("text") String text,
//             @RequestParam("chatId") String chatId,
//             @RequestParam("mediaType") String mediaType,
//             @RequestParam("media") MultipartFile media) {
//         try {
//             byte[] mediaBytes = media.getBytes();
            
//             Message message = chatService.saveMediaMessage(
//                 senderId, receiverId, text, mediaBytes, mediaType, chatId);
                
//             return ResponseEntity.ok(Map.of(
//                 "id", message.getChatId(),
//                 "timestamp", message.getTime().toString(),
//                 "status", "Message with media sent successfully"
//             ));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                 .body(Map.of("error", "Failed to send media message: " + e.getMessage()));
//         }
//     }
    
//     /**
//      * Get and decrypt messages for a conversation
//      */
//     @GetMapping("/conversations/{chatId}")
//     public ResponseEntity<?> getConversation(@PathVariable String chatId) {
//         try {
//             List<Message> messages = chatService.getConversationMessages(chatId);
//             return ResponseEntity.ok(messages);
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                 .body(Map.of("error", "Failed to retrieve conversation: " + e.getMessage()));
//         }
//     }
    
//     /**
//      * Get and decrypt messages between two users
//      */
//     @GetMapping("/messages")
//     public ResponseEntity<?> getMessagesBetweenUsers(
//             @RequestParam String senderId,
//             @RequestParam String receiverId) {
//         try {
//             List<Message> messages = chatService.getMessagesBetweenUsers(senderId, receiverId);
//             return ResponseEntity.ok(messages);
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                 .body(Map.of("error", "Failed to retrieve messages: " + e.getMessage()));
//         }
//     }
    
//     /**
//      * Get and decrypt messages for a user's inbox
//      */
//     @GetMapping("/messages/inbox/{userId}")
//     public ResponseEntity<?> getUserInbox(@PathVariable String userId) {
//         try {
//             List<Message> messages = chatService.getMessagesForUser(userId);
//             return ResponseEntity.ok(messages);
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                 .body(Map.of("error", "Failed to retrieve inbox: " + e.getMessage()));
//         }
//     }
    
//     /**
//      * Get decrypted media content from a message
//      */
//     @GetMapping("/messages/{messageId}/media")
//     public ResponseEntity<?> getMediaContent(@PathVariable String messageId) {
//         try {
//             byte[] mediaContent = chatService.getDecryptedMedia(messageId);
//             return ResponseEntity.ok(mediaContent);
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                 .body(Map.of("error", "Failed to retrieve media: " + e.getMessage()));
//         }
//     }
// }