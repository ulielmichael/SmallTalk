package mu.smalltalk.repositoriy;

import mu.smalltalk.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByChatId(String chatId);
    List<Message> findBySenderIDAndReceiverID(String senderId, String receiverId);
    List<Message> findByReceiverID(String receiverId);
}