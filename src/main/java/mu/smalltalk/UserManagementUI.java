package mu.smalltalk;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import mu.smalltalk.Services.GroupService;
import mu.smalltalk.Services.UserService;
import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.User;
import mu.smalltalk.repositoriy.UserRepository;

/**
 * UI component for managing users and groups
 */
public class UserManagementUI extends VerticalLayout {
    
    private final ComboBox<User> userSelector;
    private final Button createGroupButton;
    private final Button startDMButton;
    private final UserRepository userRepository;
    
    @Autowired
    public UserManagementUI(UserRepository userRepository) {
        this.userRepository = userRepository;
        
        // Get current user
        User currentUser = UserService.getAuthenticatedUser();
        
        // User selector
        userSelector = new ComboBox<>("Select User");
        userSelector.setItemLabelGenerator(User::getFullName);
        refreshUserList();
        
        // Create group button
        createGroupButton = new Button("Create New Group");
        createGroupButton.addClickListener(e -> showCreateGroupDialog());
        
        // Start DM button
        startDMButton = new Button("Start Direct Message");
        startDMButton.addClickListener(e -> startDirectMessage());
        startDMButton.setEnabled(false);
        
        // Enable DM button when a user is selected
        userSelector.addValueChangeListener(event -> {
            User selectedUser = event.getValue();
            startDMButton.setEnabled(selectedUser != null && !selectedUser.getId().equals(currentUser.getId()));
        });
        
        // Create toolbar
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.add(userSelector, startDMButton, createGroupButton);
        toolbar.setSpacing(true);
        
        add(new H3("User Management"), toolbar);
    }

    /**
     * Start a direct message with the selected user
     */
    private void startDirectMessage() {
        User selectedUser = userSelector.getValue();
        User currentUser = UserService.getAuthenticatedUser();
        
        if (selectedUser == null) {
            Notification.show("Please select a user first");
            return;
        }
        
        // Get or create DM group between the two users
        Group dmGroup = GroupService.getOrCreateDirectMessageGroup(
                currentUser.getId(), 
                selectedUser.getId());
        
        // Navigate to chat with this group selected
        navigateToChat(dmGroup.getId());
    }
    
    /**
     * Show dialog for creating a new group
     */
    private void showCreateGroupDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");
        
        TextField groupNameField = new TextField("Group Name");
        groupNameField.setWidth("100%");
        
        Button createButton = new Button("Create");
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        createButton.addClickListener(e -> {
            String groupName = groupNameField.getValue().trim();
            if (groupName.isEmpty()) {
                Notification.show("Please enter a group name");
                return;
            }
            
            User currentUser = UserService.getAuthenticatedUser();
            Group newGroup = GroupService.createGroup(groupName, currentUser.getId());
            
            dialog.close();
            
            // Navigate to the chat with the new group selected
            navigateToChat(newGroup.getId());
            
            Notification.show("Group created: " + groupName, 
                    2000, Notification.Position.BOTTOM_CENTER);
        });
        
        
        HorizontalLayout buttons = new HorizontalLayout(createButton, cancelButton);
        buttons.setSpacing(true);
        
        VerticalLayout layout = new VerticalLayout(
                new H3("Create New Group"), 
                groupNameField, 
                buttons);
        layout.setSpacing(true);
        layout.setPadding(true);
        
        dialog.add(layout);
        dialog.open();
    }
    /**
     * Navigate to the chat view with a specific group selected
     */
    private void navigateToChat(String groupId) {
        UI.getCurrent().navigate("chat/" + groupId);
    }
    
    /**
     * Refresh the list of users in the selector
     */
    private void refreshUserList() {
        User currentUser = UserService.getAuthenticatedUser();
        List<User> allUsers = getAllUsers();
        
        // Exclude the current user from the list
        allUsers.removeIf(user -> user.getId().equals(currentUser.getId()));
        
        userSelector.setItems(allUsers);
    }
    
    /**
     * Update a user's profile picture by email
     * @param email The user's email
     * @param profilePicData The profile picture data
     */
    public void updateProfilePictureByEmail(String email, String profilePicData) {
        if (email != null && !email.trim().isEmpty()) {
            java.util.Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setProfilePic(profilePicData);
                userRepository.save(user);
            } else {
                throw new RuntimeException("User not found with email: " + email);
            }
        } else {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
    }

    /**
     * Get all users from the repository
     * @return List of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}