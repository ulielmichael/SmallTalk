package mu.smalltalk.repositoriy;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import mu.smalltalk.entitis.Group;
// import java.util.Optional;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> 
{
    List<Group> findByUsersContaining(String userId);

}