package mu.smalltalk.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.User;
import mu.smalltalk.repositoriy.GroupRepository;
import mu.smalltalk.repositoriy.UserRepository;

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
        System.out.println("User Groups for " + userId + ": " + groups);
        return groups;
    }
    
}

