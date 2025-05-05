package mu.smalltalk.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.User;

public class GroupService {
    // Map to store groups by their ID
    private static final Map<String, Group> groups = new HashMap<>();
    
    // Map to store group memberships (groupId -> list of userIds)
    private static final Map<String, List<String>> groupMembers = new HashMap<>();
    
    // Map to store user's groups (userId -> list of groupIds)
    private static final Map<String, List<String>> userGroups = new HashMap<>();
    
    /**
     * Create a new group
     * @param name Group name
     * @param creatorId User ID of the creator
     * @return The created group
     */
    public static Group createGroup(String name, String creatorId) {
        String groupId = UUID.randomUUID().toString();
        Group group = new Group(groupId, name, creatorId, groupId, System.currentTimeMillis());
        
        groups.put(groupId, group);
        
        // Initialize empty members list
        groupMembers.put(groupId, new ArrayList<>());
        
        // Add creator as first member
        addUserToGroup(creatorId, groupId);
        
        System.out.println("Created group: " + group.getName() + " (ID: " + groupId + ") with creator: " + creatorId);
        
        return group;
    }
    
    /**
     * Add a user to a group
     * @param userId User ID to add
     * @param groupId Group ID to add the user to
     * @return true if added successfully, false otherwise
     */
    public static boolean addUserToGroup(String userId, String groupId) {
        if (!groups.containsKey(groupId)) {
            System.err.println("Group not found: " + groupId);
            return false;
        }
        
        // Add user to group members
        List<String> members = groupMembers.computeIfAbsent(groupId, k -> new ArrayList<>());
        if (!members.contains(userId)) {
            members.add(userId);
        }
        
        // Add group to user's groups
        List<String> groups = userGroups.computeIfAbsent(userId, k -> new ArrayList<>());
        if (!groups.contains(groupId)) {
            groups.add(groupId);
        }
        
        return true;
    }
   
    
    /**
     * Remove a user from a group
     * @param userId User ID to remove
     * @param groupId Group ID to remove the user from
     * @return true if removed successfully, false otherwise
     */
    public static boolean removeUserFromGroup(String userId, String groupId) {
        if (!groups.containsKey(groupId)) {
            return false;
        }
        
        // Remove from group members
        List<String> members = groupMembers.get(groupId);
        if (members != null) {
            members.remove(userId);
        }
        
        // Remove from user's groups
        List<String> userGroupList = userGroups.get(userId);
        if (userGroupList != null) {
            userGroupList.remove(groupId);
        }
        
        return true;
    }
    
    /**
     * Get all members of a group
     * @param groupId Group ID
     * @return List of user IDs who are members of the group
     */
    public static List<String> getGroupMembers(String groupId) {
        List<String> members = groupMembers.get(groupId);
        return members != null ? new ArrayList<>(members) : new ArrayList<>();
    }
    
    /**
     * Get all groups a user is a member of
     * @param userId User ID
     * @return List of groups the user is a member of
     */
    public static List<Group> getUserGroups(String userId) {
        List<String> groupIds = userGroups.get(userId);
        if (groupIds == null) {
            return new ArrayList<>();
        }
        
        return groupIds.stream()
                .map(groups::get)
                .filter(group -> group != null)
                .collect(Collectors.toList());
    }
    
    /**
     * Get a group by its ID
     * @param groupId Group ID
     * @return The group, or null if not found
     */
    public static Group getGroupById(String groupId) {
        return groups.get(groupId);
    }
    
    /**
     * Create a direct message group between two users
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @return The created direct message group
     */
    public static Group createDirectMessageGroup(String user1Id, String user2Id) {
        // Get user names for the group name
        User user1 = UserService.getUserById(user1Id);
        User user2 = UserService.getUserById(user2Id);
        
        String user1Name = user1 != null ? user1.getFullName() : "User1";
        String user2Name = user2 != null ? user2.getFullName() : "User2";
        
        // Create group name based on the two users
        String groupName = "DM: " + user1Name + " & " + user2Name;
        
        // Create the group with user1 as creator
        Group dmGroup = createGroup(groupName, user1Id);
        
        // Add user2 to the group
        addUserToGroup(user2Id, dmGroup.getId());
        
        return dmGroup;
    }
    
    /**
     * Find an existing direct message group between two users
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @return The existing DM group or null if none exists
     */
    public static Group findDirectMessageGroup(String user1Id, String user2Id) {
        // Get all groups user1 belongs to
        List<Group> user1Groups = getUserGroups(user1Id);
        
        // Check if any of those groups is a DM group with user2
        for (Group group : user1Groups) {
            List<String> members = getGroupMembers(group.getId());
            
            // If it's a 2-person group and contains both users, it's a DM group
            if (members.size() == 2 && 
                members.contains(user1Id) && 
                members.contains(user2Id) &&
                group.getName().startsWith("DM:")) {
                return group;
            }
        }
        
        // No existing DM group found
        return null;
    }
    
    /**
     * Get or create a direct message group between two users
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @return The DM group (existing or newly created)
     */
    public static Group getOrCreateDirectMessageGroup(String user1Id, String user2Id) {
        // Try to find existing DM group
        Group existingGroup = findDirectMessageGroup(user1Id, user2Id);
        if (existingGroup != null) {
            return existingGroup;
        }
        
        // If not found, create new DM group
        return createDirectMessageGroup(user1Id, user2Id);
    }
    
    /**
     * Returns a list of users in a specific group
     * @param groupId The ID of the group
     * @return List of users in the group
     */
    public static List<User> getGroupUsers(String groupId) {
        // Get the group members (user IDs)
        List<String> memberIds = getGroupMembers(groupId);
        
        // Convert user IDs to User objects
        List<User> users = new ArrayList<>();
        for (String userId : memberIds) {
            User user = UserService.getUserById(userId);
            if (user != null) {
                users.add(user);
            }
        }
        
        return users;
    }
}