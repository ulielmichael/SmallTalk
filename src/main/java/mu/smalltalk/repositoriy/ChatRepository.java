// package mu.smalltalk.repository;

// import mu.smalltalk.Chat;
// import org.springframework.data.mongodb.repository.MongoRepository;
// import org.springframework.stereotype.Repository;
// import java.util.List;

// @Repository
// public interface ChatRepository extends MongoRepository<Chat, String> {
//     List<Chat> findByParticipantsContaining(String userId);
// }