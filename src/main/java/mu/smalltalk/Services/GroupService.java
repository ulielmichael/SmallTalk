package mu.smalltalk.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.User;
import mu.smalltalk.repositoriy.GroupRepository;
import mu.smalltalk.repositoriy.UserRepository;

@Service
public class GroupService {
    
    private static GroupRepository groupRepository;
    private static UserRepository userRepository;
    private static MongoDbSerivce mongoDbService;
    
    @Autowired
    public GroupService(GroupRepository groupRepository, UserRepository userRepository, MongoDbSerivce mongoDbService) {
        GroupService.groupRepository = groupRepository;
        GroupService.userRepository = userRepository;
        GroupService.mongoDbService = mongoDbService;
    }
    
    /**
     * Get all groups that a user belongs to
     */
    public static List<Group> getUserGroups(String userId) {
        if (mongoDbService != null) {
            return mongoDbService.getUserGroups(userId);
        }
        // Fall back to repository if needed
        return groupRepository.findAll().stream()
                .filter(group -> group.getUsers().contains(userId))
                .collect(Collectors.toList());
    }
    
    /**
     * Create a new group with the specified name and add the creator as the first member
     */
    public static Group createGroup(String groupName, String creatorId) {
        // Check if group already exists
        Optional<Group> existingGroup = groupRepository.findById(groupName);
        if (existingGroup.isPresent()) {
            throw new IllegalArgumentException("Group with name '" + groupName + "' already exists");
        }
        
        // Create new group
        Group newGroup = new Group(groupName);
        newGroup.addUserId(creatorId);
        
        // Save to database
        if (mongoDbService != null) {
            mongoDbService.addGroupToDB(newGroup);
        } else {
            groupRepository.save(newGroup);
        }
        
        return newGroup;
    }
    
   
    public static boolean addUserToGroup(String userEmail, String groupId) {
        // Verify user exists
        Optional<User> user = userRepository.findById(userEmail);
        if (user.isEmpty()) {
            return false;
        }
        
        // Get the group
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return false;
        }
        
        Group group = groupOpt.get();
        
        // Check if user is already in the group
        if (group.getUsers().contains(userEmail)) {
            return true; // User is already in the group
        }
        
        // Add user to group
        group.addUserId(userEmail);
        groupRepository.save(group);
        
        return true;
    }
    
    
    public static Group getGroupById(String groupId) {
        return groupRepository.findById(groupId).orElse(null);
    }
    
   
    public static boolean removeUserFromGroup(String userId, String groupName) {
        Optional<Group> groupOpt = groupRepository.findById(groupName);
        if (groupOpt.isEmpty()) {
            return false;
        }
        
        Group group = groupOpt.get();
        ArrayList<String> users = group.getUsers();
        
        if (!users.contains(userId)) {
            return false; // User not in group
        }
        
        users.remove(userId);
        group.setUsers(users);
        groupRepository.save(group);
        
        return true;
    }
    
 
    public static List<User> getGroupMembers(String groupName) {
        Optional<Group> groupOpt = groupRepository.findById(groupName);
        if (groupOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        Group group = groupOpt.get();
        List<String> userIds = group.getUsers();
        
       
        return userIds.stream()
                .map(id -> userRepository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    

    public static boolean isUserInGroup(String userId, String groupName) {
        Optional<Group> groupOpt = groupRepository.findById(groupName);
        if (groupOpt.isEmpty()) {
            return false;
        }
        
        return groupOpt.get().getUsers().contains(userId);
    }
    

    public static List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        groupRepository.findAll().forEach(groups::add);
        return groups;
    }
    
    
    public static boolean deleteGroup(String groupName) {
        if (!groupRepository.existsById(groupName)) {
            return false;
        }
        
        groupRepository.deleteById(groupName);
        return true;
    }
}