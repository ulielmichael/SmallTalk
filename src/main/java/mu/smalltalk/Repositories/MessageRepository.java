package mu.smalltalk.Repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import mu.smalltalk.entitis.Message;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    // Find messages by chat ID
    List<Message> findByChatId(String chatId);
    
    // Find messages by chat ID ordered by time (descending) with pagination
    @Query(value = "{'chatId': ?0}", sort = "{'time': -1}")
    List<Message> findByChatIdOrderByTimeDesc(String chatId, Pageable pageable);
    
    // Count messages by chat ID
    long countByChatId(String chatId);
    
    // Find messages by chat ID with pagination support
    @Query(value = "{'chatId': ?0}", sort = "{'time': -1}")
    List<Message> findByChatIdWithPagination(String chatId, Pageable pageable);
    
    // Find messages by sender and receiver
    List<Message> findBySenderIDAndReceiverID(String senderID, String receiverID);
    
    // Find messages by receiver (for a user's inbox)
    List<Message> findByReceiverID(String receiverID);
    
    // Find messages by sender (for a user's sent items)
    List<Message> findBySenderID(String senderID);
    
    // Find recent messages by chat ID (ordered by time descending)
    List<Message> findByChatIdOrderByTimeDesc(String chatId);
}