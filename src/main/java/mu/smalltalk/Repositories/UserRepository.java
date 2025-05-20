package mu.smalltalk.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import mu.smalltalk.entitis.User;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> 
{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}