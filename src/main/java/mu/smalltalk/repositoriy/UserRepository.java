package mu.smalltalk.repositoriy;

import org.springframework.data.mongodb.repository.MongoRepository;

import mu.smalltalk.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}