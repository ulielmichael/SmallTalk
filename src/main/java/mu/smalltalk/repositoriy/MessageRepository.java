package mu.smalltalk.repositoriy;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import mu.smalltalk.Message;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    // Find messages by chat ID
    List<Message> findByChatId(String chatId);
    
    // Find messages by sender and receiver
    List<Message> findBySenderIDAndReceiverID(String senderID, String receiverID);
    
    // Find messages by receiver (for a user's inbox)
    List<Message> findByReceiverID(String receiverID);
    
    // Find messages by sender (for a user's sent items)
    List<Message> findBySenderID(String senderID);
}