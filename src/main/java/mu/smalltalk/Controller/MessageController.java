// package mu.smalltalk.Controller;



// import mu.smalltalk.Message;
// import mu.smalltalk.Services.ChatService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/api/messages")
// public class MessageController {

//     @Autowired
//     private mu.smalltalk.Services.ChatService chatService;

//     @PostMapping("/send")
//     public ResponseEntity<Message> sendMessage(@RequestBody Message message) {
//         return ResponseEntity.ok(chatService.saveMessage(message));
//     }

//     @GetMapping("/conversation/{chatId}")
//     public ResponseEntity<List<Message>> getConversation(@PathVariable String chatId) throws Exception {
//         return ResponseEntity.ok(chatService.getConversationMessages(chatId));
//     }

//     @GetMapping("/user/{userId}")
//     public ResponseEntity<List<Message>> getMessagesForUser(@PathVariable String userId) throws Exception {
//         return ResponseEntity.ok(chatService.getMessagesForUser(userId));
//     }

//     @GetMapping("/between")
//     public ResponseEntity<List<Message>> getMessagesBetweenUsers(
//             @RequestParam String senderId,
//             @RequestParam String receiverId) throws Exception {
//         return ResponseEntity.ok(chatService.getMessagesBetweenUsers(senderId, receiverId));
//     }
// }