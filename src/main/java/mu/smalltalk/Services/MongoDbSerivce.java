package mu.smalltalk.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.User;
import mu.smalltalk.Repositories.GroupRepository;
import mu.smalltalk.Repositories.UserRepository;

@Service
public class MongoDbSerivce 
{
    private UserRepository userRepo;
    private GroupRepository groupRepo;

    public MongoDbSerivce(UserRepository userRepo, GroupRepository groupRepo) 
    {
        this.userRepo = userRepo;
        this.groupRepo = groupRepo;
    }
 
    public void addUsertoDB(User user)
    {
        userRepo.insert(user);
    }

    public void addGroupToDB(Group group) 
    {
       groupRepo.insert(group);
    }
  

    public List<Group> getUserGroups(String userId) {
        List<Group> groups = groupRepo.findByUsersContaining(userId);
        return groups;
    }
    
}

