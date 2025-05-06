package mu.smalltalk.Pages;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

import mu.smalltalk.Aes256;
import mu.smalltalk.Services.EncryptionService;
import mu.smalltalk.Services.GlobalMessageBroadcaster;
import mu.smalltalk.Services.MongoDbSerivce;
// import mu.smalltalk.Services.GroupService;
import mu.smalltalk.Services.UserService;
import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.User;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Route("chat")
public class GroupChat extends VerticalLayout  {

    private final MessageInput messageInput;
    private final Upload mediaUpload;
    private final Aes256 aes;
    private final EncryptionService encryptionService;
    private final VerticalLayout chatContainer;
    private final VerticalLayout userListContainer; // Added user list container
    private static final int CHAT_HEIGHT = 500;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String sessionId;
    private Registration broadcasterRegistration;
    private ComboBox<Group> groupSelector;
    private String currentGroupId = null;



    private int lastSeenMessageCount = 0;
    
    // Add a refresh button for the chat
    private Button refreshButton;
    
    // Add theme toggle button
    private Button themeToggleButton;
    
    // Track current theme
    private boolean isDarkMode = false;

    public GroupChat() {
        sessionId = initializeSessionId();


        aes = initializeEncryption();
        encryptionService = new EncryptionService(aes);

        messageInput = new MessageInput();
        chatContainer = new VerticalLayout();
        userListContainer = new VerticalLayout(); // Initialize user list container

        chatContainer.setWidthFull();
        chatContainer.setHeight(CHAT_HEIGHT, Unit.PIXELS);
        
        // Default to light mode
        applyLightMode();
        
        chatContainer.getStyle().set("overflow-y", "auto");
        chatContainer.getStyle().set("border", "1px solid #ddd");

        // Configure user list container
        userListContainer.setWidthFull();
        userListContainer.getStyle().set("padding", "10px");
        userListContainer.getStyle().set("margin-bottom", "10px");
        userListContainer.getStyle().set("border", "1px solid #ddd");
        userListContainer.getStyle().set("border-radius", "4px");

        messageInput.setWidthFull();

        MemoryBuffer buffer = new MemoryBuffer();
        mediaUpload = new Upload(buffer);
        configureMediaUpload(mediaUpload, buffer);
        
        // Create refresh button
        refreshButton = new Button("Refresh Chat");
        // refreshButton.addClickListener(e -> refreshChatHistory());
        
        // Create theme toggle button
        themeToggleButton = new Button("DARK MODE");
        themeToggleButton.getStyle().set("background-color", "#333");
        themeToggleButton.getStyle().set("color", "white");
        themeToggleButton.addClickListener(e -> toggleTheme());
        
        // Create header with title and theme toggle
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        
        // Use authenticated user's email or name if available
        User currentUser = UserService.getAuthenticatedUser();
        String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;
        header.add(new H2(displayName), themeToggleButton);
        
        // Create group selector
        groupSelector = new ComboBox<>("Select Group");
        groupSelector.setItemLabelGenerator(Group::getName);
        groupSelector.setWidthFull();
        
        // Create new group button
        Button newGroupButton = new Button("Create New Group");
        // newGroupButton.addClickListener(e -> createNewGroup());
        
        // // Fill the group selector with the groups the user belongs to
        // if (currentUser != null) {
        //     List<Group> userGroups = GroupService.getUserGroups(currentUser.getId());
            
        //     // If no groups exist, create a default group for this user
        //     if (userGroups.isEmpty()) {
        //         System.out.println("No groups found for user " + currentUser.getId() + ", creating a default group");
        //         Group defaultGroup = GroupService.createGroup("Default Group", currentUser.getId());
        //         userGroups = GroupService.getUserGroups(currentUser.getId());
        //     }
            
        //     groupSelector.setItems(userGroups);
            
        //     // Set the first group as default if available
        //     if (!userGroups.isEmpty()) {
        //         System.out.println("Setting default group: " + userGroups.get(0).getName());
        //         groupSelector.setValue(userGroups.get(0));
        //         currentGroupId = userGroups.get(0).getId();
        //     }
        // }
        
        groupSelector.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                // currentGroupId = event.getValue().get();
                // refreshChatHistory();
                // updateUserList(); // Update user list when group changes
                
                // Update the chat title with the group name
                getChildren().forEach(component -> {
                    if (component instanceof HorizontalLayout) {
                        HorizontalLayout layout = (HorizontalLayout) component;
                        layout.getChildren().forEach(child -> {
                            if (child instanceof H2) {
                                ((H2) child).setText(displayName + " - " + event.getValue().getName());
                            }
                        });
                    }
                });
                
                // Update URL to reflect the selected group
                if (UI.getCurrent() != null) {
                    UI.getCurrent().navigate("chat/" + currentGroupId);
                }
            }
        });
        
        // Create action buttons layout
        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.add(refreshButton, groupSelector, newGroupButton);
        actionButtons.setWidthFull();
        actionButtons.setSpacing(true);
        
        // Create toolbar with home and logout buttons
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.END);

        Button homeButton = new Button("Home");
        homeButton.addClickListener(e -> UI.getCurrent().navigate(""));  // Navigate to home page

        Button logoutButton = new Button("Logout");
        logoutButton.addClickListener(e -> {
            UserService.clearAuthenticatedUser();
            VaadinSession.getCurrent().getSession().invalidate();
            UI.getCurrent().getPage().executeJs(
                "localStorage.removeItem('chat-session-id');");
            UI.getCurrent().navigate("");
        });

        toolbar.add(homeButton, logoutButton);

        // Add a section for user list
        H3 userListHeader = new H3("Group Members");
        
        // Add components to main layout
        add(toolbar, header, userListHeader, userListContainer, chatContainer, messageInput, mediaUpload, actionButtons);

        setupMessageHandler();
        setupCrossBrowserCommunication();
        loadThemePreference();
    }

 
// @Override
// public void beforeEnter(BeforeEnterEvent event) {
//     if (!UserService.isUserAuthenticated()) {
//         event.forwardTo("login");  
        
//         Notification.show("Please log in to access the chat", 
//                        3000, Notification.Position.MIDDLE);
//         return;
//     }
    
    // Handle group ID parameter if present
    // List<String> segments = event.getLocation().getSegments();
    // if (segments.size() > 1) {
    //     String groupIdParam = segments.get(1);
    //     if (groupIdParam != null && !groupIdParam.isEmpty()) {
    //         // Check if user is allowed to join this group
    //         User currentUser = UserService.getAuthenticatedUser();
            // if (currentUser != null) {
            //     // final List<Group> userGroups = GroupService.getUserGroups(currentUser.getId());
                
            //     // Find the target group
            //     final Group foundGroup = userGroups.stream()
            //         .filter(group -> group.getId().equals(groupIdParam))
            //         .findFirst()
            //         .orElse(null);
                
            //     if (foundGroup != null) {
            //         currentGroupId = groupIdParam;
                    
            //         // Use access() with runnable to ensure UI update is performed correctly
            //         UI.getCurrent().access(() -> {
            //             groupSelector.setValue(foundGroup);
            //             loadExistingMessages();
            //             updateUserList();
            //         });
            //     } else {
            //         Notification.show("You are not a member of this group", 
            //             3000, Notification.Position.MIDDLE);
            //         event.forwardTo("chat");
            //     }
            // }
//         }
//     } else {
//         // If no group is specified, try to select the first available group
//         User currentUser = UserService.getAuthenticatedUser();
//         if (currentUser != null) {
//             final List<Group> userGroups = GroupService.getUserGroups(currentUser.getId());
            
//             // Create a default group if no groups exist
//             if (userGroups.isEmpty()) {
//                 System.out.println("No groups found for user " + currentUser.getId() + ", creating a default group");
//                 final Group defaultGroup = GroupService.createGroup("Default Group", currentUser.getId());
//                 final List<Group> updatedUserGroups = GroupService.getUserGroups(currentUser.getId());
                
//                 UI.getCurrent().access(() -> {
//                     if (!updatedUserGroups.isEmpty()) {
//                         Group firstGroup = updatedUserGroups.get(0);
//                         currentGroupId = firstGroup.getId();
//                         groupSelector.setItems(updatedUserGroups); // Refresh the items
//                         groupSelector.setValue(firstGroup);
//                         loadExistingMessages();
//                         updateUserList();
//                     }
//                 });
//             } else {
//                 UI.getCurrent().access(() -> {
//                     if (!userGroups.isEmpty()) {
//                         Group firstGroup = userGroups.get(0);
//                         currentGroupId = firstGroup.getId();
//                         groupSelector.setItems(userGroups); // Refresh the items
//                         groupSelector.setValue(firstGroup);
//                         loadExistingMessages();
//                         updateUserList();
//                     }
//                 });
//             }
//         }
//     }
// }

    // private void createNewGroup() {
    //     User currentUser = UserService.getAuthenticatedUser();
    //     if (currentUser != null) {
    //         // Create a simple dialog to enter group name
    //         // In a real app, this would be a proper dialog component
    //         String groupName = "New Group " + new Date().getTime();
    //         // Group newGroup = GroupService.createGroup(groupName, currentUser.getId());
            
    //         if (newGroup != null) {
    //             // Refresh the group selector
    //             List<Group> userGroups = GroupService.getUserGroups(currentUser.getId());
    //             groupSelector.setItems(userGroups);
    //             groupSelector.setValue(newGroup);
                
    //             Notification.show("Group created: " + groupName, 
    //                 2000, Notification.Position.MIDDLE);
    //         }
    //     }
    // }

    // private void updateUserList() {
    //     if (currentGroupId == null) {
    //         return;
    //     }
        
    //     User currentUser = UserService.getAuthenticatedUser();
    //     if (currentUser == null) {
    //         return;
    //     }
        
    //     // Clear previous list
    //     userListContainer.removeAll();
        
    //     // Get users for the current group
    //     List<User> groupUsers = GroupService.getGroupUsers(currentGroupId);
        
    //     // Display users in the container
    //     if (groupUsers.isEmpty()) {
    //         Div noUsersDiv = new Div();
    //         noUsersDiv.setText("No members in this group");
    //         userListContainer.add(noUsersDiv);
    //     } else {
    //         HorizontalLayout userLayout = new HorizontalLayout();
    //         userLayout.setWidthFull();
    //         userLayout.getStyle().set("flex-wrap", "wrap");
            
    //         for (User user : groupUsers) {
    //             Div userDiv = new Div();
    //             userDiv.setText(user.getFullName());
    //             userDiv.getStyle().set("margin", "5px");
    //             userDiv.getStyle().set("padding", "5px 10px");
    //             userDiv.getStyle().set("border-radius", "15px");
                
    //             // Highlight current user
    //             if (user.getId().equals(currentUser.getId())) {
    //                 userDiv.getStyle().set("background-color", isDarkMode ? "#4a5568" : "#bee3f8");
    //                 userDiv.getStyle().set("font-weight", "bold");
    //             } else {
    //                 userDiv.getStyle().set("background-color", isDarkMode ? "#2d3748" : "#e2e8f0");
    //             }
                
    //             userLayout.add(userDiv);
    //         }
            
    //         userListContainer.add(userLayout);
    //     }
    // }

    private void loadThemePreference() {
        UI.getCurrent().getPage().executeJs(
                "return localStorage.getItem('chat-theme-preference');")
                .then(String.class, result -> {
                    if ("dark".equals(result)) {
                        isDarkMode = true;
                        applyDarkMode();
                        themeToggleButton.setText("LIGHT MODE");
                        themeToggleButton.getStyle().set("background-color", "#f0f0f0");
                        themeToggleButton.getStyle().set("color", "#333");
                    }
                });
    }
    
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        
        if (isDarkMode) {
            applyDarkMode();
            themeToggleButton.setText("LIGHT MODE");
            themeToggleButton.getStyle().set("background-color", "#f0f0f0");
            themeToggleButton.getStyle().set("color", "#333");
        } else {
            applyLightMode();
            themeToggleButton.setText("DARK MODE");
            themeToggleButton.getStyle().set("background-color", "#333");
            themeToggleButton.getStyle().set("color", "white");
        }
        
        UI.getCurrent().getPage().executeJs(
                "localStorage.setItem('chat-theme-preference', $0);", isDarkMode ? "dark" : "light");
                
        // refreshChatHistory();
        // updateUserList(); // Update user list when theme changes
    }
    
    private void applyDarkMode() {
        getStyle().set("background-color", "#2c2c2c");
        getStyle().set("color", "#f0f0f0");
        
        chatContainer.getStyle().set("background-color", "#1e1e1e");
        chatContainer.getStyle().set("border", "1px solid #444");
        
        userListContainer.getStyle().set("background-color", "#1e1e1e");
        userListContainer.getStyle().set("border", "1px solid #444");
        
        messageInput.getStyle().set("background-color", "#333");
        messageInput.getStyle().set("color", "white");
    }
    
    private void applyLightMode() {
        getStyle().set("background-color", "#f8f8f8");
        getStyle().set("color", "#333");
        
        chatContainer.getStyle().set("background-color", "white");
        chatContainer.getStyle().set("border", "1px solid #ddd");
        
        userListContainer.getStyle().set("background-color", "white");
        userListContainer.getStyle().set("border", "1px solid #ddd");
        
        messageInput.getStyle().set("background-color", "white");
        messageInput.getStyle().set("color", "#333");
    }

    // @Override
    // protected void onAttach(AttachEvent attachEvent) {
    //     super.onAttach(attachEvent);
    
    //     registerWithBroadcaster();
    
    //     // Load existing messages after registration is complete
    //     UI.getCurrent().access(() -> {
    //         // Check if we need to select a group
    //         if (currentGroupId == null && groupSelector != null && groupSelector.getValue() == null) {
    //             User currentUser = UserService.getAuthenticatedUser();
    //             if (currentUser != null) {
    //                 List<Group> userGroups = GroupService.getUserGroups(currentUser.getId());
                    
    //                 // Create a default group if no groups exist
    //                 if (userGroups.isEmpty()) {
    //                     System.out.println("No groups found for user " + currentUser.getId() + ", creating a default group");
    //                     Group defaultGroup = GroupService.createGroup("Default Group", currentUser.getId());
    //                     userGroups = GroupService.getUserGroups(currentUser.getId());
    //                 }
                    
    //                 if (!userGroups.isEmpty()) {
    //                     currentGroupId = userGroups.get(0).getId();
    //                     groupSelector.setItems(userGroups); // Refresh the items
    //                     groupSelector.setValue(userGroups.get(0));
    //                 }
    //             }
    //         }
            
    //         loadExistingMessages();
    //         updateUserList();
    //     });
        
    //     checkForNewMessages();
    
    //     UI ui = attachEvent.getUI();
    //     if (ui != null) {
    //         ui.setPollInterval(500); 
    //     }
    // }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    // private void loadExistingMessages() {
    //     if (currentGroupId == null) {
    //         return;
    //     }
        
    //     // List<String> existingMessages = Chatstorage.getGroupMessages(currentGroupId);
        
    //     chatContainer.removeAll();
        
    //     for (String message : existingMessages) {
    //         addMessageToChat(message);
    //     }
    //     lastSeenMessageCount = existingMessages.size();

    //     scrollToBottom();
    // }
    
    // private void refreshChatHistory() {
    //     UI ui = UI.getCurrent();
    //     if (ui != null && ui.isAttached()) {
    //         ui.access(() -> {
    //             loadExistingMessages();
    //             updateUserList();
    //             ui.push();
    //             Notification.show("Chat refreshed", 2000, Notification.Position.BOTTOM_CENTER);
    //         });
    //     }
    // }

    private void setupMessageHandler() {
        UI currentUI = UI.getCurrent();
        VaadinSession currentSession = VaadinSession.getCurrent();

        messageInput.addSubmitListener(submitEvent -> {
            if (currentGroupId == null) {
                Notification.show("Please select a group first", 2000, Notification.Position.MIDDLE);
                return;
            }
            
            String message = submitEvent.getValue();
            String timestamp = dateFormat.format(new Date());
            
            User currentUser = UserService.getAuthenticatedUser();
            String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;
            
            System.out.println("Received message from " + displayName + " in group " + currentGroupId + ": " + message);

            String formattedMessage = "[" + timestamp + "] " + displayName + ": " + message;
            sendGroupMessage(formattedMessage, currentGroupId);
            
            encryptionService.encryptStringAsync(message)
                    .thenAccept(encryptedMessage -> {
                        if (currentUI.isAttached() && !currentSession.getSession().isNew()) {
                            currentSession.lock();
                            try {
                                currentUI.access(() -> {
                                    String encTimestamp = dateFormat.format(new Date());
                                    String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessage);

                                    String encryptedFormattedMessage = "[" + encTimestamp + "] " + displayName + ": <b>Encrypted:</b> "
                                            + encodedMessage;
                                    
                                    sendGroupMessage(encryptedFormattedMessage, currentGroupId);

                                    decryptAndDisplayMessage(encodedMessage, currentGroupId);
                                    currentUI.push();
                                });
                            } finally {
                                currentSession.unlock();
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        if (currentUI.isAttached() && !currentSession.getSession().isNew()) {
                            currentSession.lock();
                            try {
                                currentUI.access(() -> {
                                    String errorTimestamp = dateFormat.format(new Date());
                                    String errorMessage = "[" + errorTimestamp + "] " + displayName
                                            + ": <b>Error encrypting message:</b> " + ex.getMessage();
                                    
                                    sendGroupMessage(errorMessage, currentGroupId);
                                    
                                    currentUI.push();
                                });
                            } finally {
                                currentSession.unlock();
                            }
                        }
                        return null;
                    });
        });
    }

    private void setupCrossBrowserCommunication() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.getPage().executeJs(
                    "window.addEventListener('storage', function(e) {" +
                            "   if (e.key === 'chat-update-trigger') {" +
                            "       $0.$server.handleChatUpdate(e.newValue);" +
                            "   }" +
                            "});",
                    getElement());
                    
            ui.getPage().executeJs(
                    "function notifyOtherWindows() {" +
                    "   localStorage.setItem('chat-update-trigger', Date.now().toString());" +
                    "}" +
                    "window.notifyOtherWindows = notifyOtherWindows;");
        }
    }

    // public void handleChatUpdate(String timestamp) {
    //     UI ui = UI.getCurrent();
    //     if (ui != null && ui.isAttached()) {
    //         ui.access(() -> {
    //             checkForNewMessages();
    //             // updateUserList();
    //             ui.push();
    //         });
    //     }
    // }

    // public void checkForNewMessages() {
    //     if (currentGroupId == null) {
    //         return;
    //     }
        
    //     UI ui = UI.getCurrent();
    //     if (ui != null && ui.isAttached()) {
    //         ui.access(() -> {
    //             // List<String> allMessages = Chatstorage.getGroupMessages(currentGroupId);
    //             int currentMessageCount = allMessages.size();

    //             if (currentMessageCount > lastSeenMessageCount) {
    //                 for (int i = lastSeenMessageCount; i < currentMessageCount; i++) {
    //                     addMessageToChat(allMessages.get(i));
    //                 }
    //                 lastSeenMessageCount = currentMessageCount;

    //                 scrollToBottom();
                    
    //                 ui.getPage().executeJs("window.notifyOtherWindows();");
                    
    //                 ui.push();
    //             }
    //         });
    //     }
    // }

    private void registerWithBroadcaster() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            if (broadcasterRegistration != null) {
                broadcasterRegistration.remove();
                broadcasterRegistration = null;
            }

            System.out.println("Registering UI: " + ui.getUIId() + " with broadcaster");

            // Using the GroupMessageConsumer interface
            // broadcasterRegistration = GlobalMessageBroadcaster.register(ui, (message, groupId) -> {
            //     if (ui.isAttached() && (currentGroupId != null && currentGroupId.equals(groupId))) {
            //         ui.access(() -> {
            //             addMessageToChat(message);
                        
            //             if (currentGroupId != null) {
            //                 lastSeenMessageCount = Chatstorage.getGroupMessages(currentGroupId).size();
            //             }
                        
            //             scrollToBottom();
            //             ui.push();
            //         });
            //     }
            // });

            if (broadcasterRegistration == null) {
                System.err.println("Failed to register with broadcaster for UI: " + ui.getUIId());
                Notification.show("Failed to connect to the chat server. Please refresh the page.",
                        3000, Notification.Position.MIDDLE);
            }
        } else {
            System.err.println("Cannot register with broadcaster - UI is null");
        }
    }
    
    public void ensureRegistration() {
        if (broadcasterRegistration == null || !GlobalMessageBroadcaster.isRegistered(UI.getCurrent())) {
            System.out.println("Re-registering with broadcaster after reconnection");
            registerWithBroadcaster();
        }

        // checkForNewMessages();
    }

    private String initializeSessionId() {
        VaadinSession session = VaadinSession.getCurrent();
        String uniqueId;
    
        User currentUser = UserService.getAuthenticatedUser();
        if (currentUser != null) {
            uniqueId = currentUser.getFullName(); 
            session.setAttribute("sessionId", uniqueId);
            
            UI.getCurrent().getPage().executeJs(
                    "localStorage.setItem('chat-session-id', $0);", uniqueId);
            
            updateSessionIdDisplay(uniqueId);
            
            return uniqueId;
        }
        
        UI.getCurrent().getPage().executeJs(
            "return localStorage.getItem('chat-session-id');")
            .then(String.class, result -> {
                if (result != null && !result.isEmpty()) {
                    session.setAttribute("sessionId", result);
                    
                    updateSessionIdDisplay(result);
                    this.sessionId = result;
                }
            });
        
        if (session.getAttribute("sessionId") == null) {
            uniqueId = "Guest-" + UUID.randomUUID().toString().substring(0, 8);
            session.setAttribute("sessionId", uniqueId);
    
            UI.getCurrent().getPage().executeJs(
                    "localStorage.setItem('chat-session-id', $0);", uniqueId);
            
            return uniqueId;
        } else {
            uniqueId = session.getAttribute("sessionId").toString();
        }
    
        return uniqueId;
    }

    private void updateSessionIdDisplay(String id) {
        getChildren().forEach(component -> {
            if (component instanceof HorizontalLayout) {
                HorizontalLayout layout = (HorizontalLayout) component;
                layout.getChildren().forEach(child -> {
                    if (child instanceof H2) {
                        // If we have a group selected, show both the username and group name
                        if (groupSelector != null && groupSelector.getValue() != null) {
                            ((H2) child).setText(id + " - " + groupSelector.getValue().getName());
                        } else {
                            ((H2) child).setText(id);
                        }
                    }
                });
            }
        });
    }
    
    private void configureMediaUpload(Upload upload, MemoryBuffer buffer) {
        Button uploadButton = new Button("Upload");
        upload.setUploadButton(uploadButton);
        upload.setAcceptedFileTypes("image/*", "audio/*");
        upload.setMaxFileSize(16 * 1024 * 1024); // 16 MB

        upload.addSucceededListener(event -> {
            if (currentGroupId == null) {
                Notification.show("Please select a group first", 2000, Notification.Position.MIDDLE);
                return;
            }
            
            UI ui = UI.getCurrent();
            VaadinSession session = VaadinSession.getCurrent();
            
            User currentUser = UserService.getAuthenticatedUser();
            String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;
            
            String fileName = event.getFileName();
            String mimeType = event.getMIMEType();
            try {
                InputStream inputStream = buffer.getInputStream();
                byte[] fileData = inputStream.readAllBytes();
                String base64Data = Base64.getEncoder().encodeToString(fileData);
                String timestamp = dateFormat.format(new Date());

                if (mimeType.startsWith("image/")) {
                    String imageHtml = "<img src='data:" + mimeType + ";base64," + base64Data +
                            "' alt='Image' style='max-width: 100%; max-height: 300px;'>";
                    sendGroupMessage("[" + timestamp + "] " + displayName + ": ✅ Image uploaded: " + fileName + "<br>"
                            + imageHtml, currentGroupId);
                } else if (mimeType.startsWith("audio/")) {
                    String audioHtml = "<audio controls><source src='data:" + mimeType + ";base64," +
                            base64Data + "' type='" + mimeType + "'></audio>";
                    sendGroupMessage("[" + timestamp + "] " + displayName + ": ✅ Audio uploaded: " + fileName + "<br>"
                            + audioHtml, currentGroupId);
                }

                final byte[] finalFileData = fileData;
                encryptionService.encryptAsync(finalFileData)
                    .thenAccept(encryptedData -> {
                        if (ui.isAttached() && !session.getSession().isNew()) {
                            session.lock();
                            try {
                                ui.access(() -> {
                                    String encTimestamp = dateFormat.format(new Date());
                                    String successMessage = "[" + encTimestamp + "] " + displayName + ": ✅ File " + fileName
                                            + " encrypted successfully";
                                    
                                    sendGroupMessage(successMessage, currentGroupId);
                                    
                                    ui.push();
                                });
                            } finally {
                                session.unlock();
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        if (ui.isAttached() && !session.getSession().isNew()) {
                            session.lock();
                            try {
                                ui.access(() -> {
                                    String errorTimestamp = dateFormat.format(new Date());
                                    String errorMessage = "[" + errorTimestamp + "] " + displayName
                                            + ": ❌ Error encrypting file: " + ex.getMessage();
                                    
                                    sendGroupMessage(errorMessage, currentGroupId);
                                    
                                    ui.push();
                                });
                            } finally {
                                session.unlock();
                            }
                        }
                        return null;
                    });
            } catch (IOException e) {
                String errorTimestamp = dateFormat.format(new Date());
                String errorMessage = "[" + errorTimestamp + "] " + displayName
                        + ": ❌ Error processing file: " + e.getMessage();
                
                sendGroupMessage(errorMessage, currentGroupId);
            }
        });
    }

    private void sendGroupMessage(String message, String groupId) {
        if (groupId == null) {
            System.err.println("Cannot send message: Group ID is null");
            return;
        }
        
        // Store message in the group's chat history
        // Chatstorage.addGroupMessage(groupId, message);
        
        // Broadcast message to all connected clients
        GlobalMessageBroadcaster.broadcastToGroup(message, groupId);
        
        // Notify other browser windows/tabs
        UI.getCurrent().getPage().executeJs("window.notifyOtherWindows();");
    }

    private void addMessageToChat(String message) {
        Div messageDiv = new Div();
        messageDiv.getElement().setProperty("innerHTML", message);
        
        // Apply theme-specific styling
        if (isDarkMode) {
            messageDiv.getStyle().set("background-color", "#2d3748");
            messageDiv.getStyle().set("color", "#e2e8f0");
        } else {
            messageDiv.getStyle().set("background-color", "#f0f0f0");
            messageDiv.getStyle().set("color", "#333");
        }
        
        messageDiv.getStyle().set("padding", "8px 12px");
        messageDiv.getStyle().set("margin-bottom", "8px");
        messageDiv.getStyle().set("border-radius", "8px");
        messageDiv.getStyle().set("word-wrap", "break-word");
        
        chatContainer.add(messageDiv);
        scrollToBottom();
    }

    private void scrollToBottom() {
        UI.getCurrent().getPage().executeJs(
                "setTimeout(function() { "
                + "  const chatContainer = document.querySelector('.chat-container'); "
                + "  if (chatContainer) { "
                + "    chatContainer.scrollTop = chatContainer.scrollHeight; "
                + "  } "
                + "}, 100);");
    }

    private void decryptAndDisplayMessage(String encodedMessage, String groupId) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encodedMessage);
            
            User currentUser = UserService.getAuthenticatedUser();
            String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;
            
            encryptionService.decryptAsync(encryptedBytes)
                .thenAccept(decryptedBytes -> {
                    String decryptedMessage = new String(decryptedBytes);
                    String timestamp = dateFormat.format(new Date());
                    String formattedMessage = "[" + timestamp + "] " + displayName + ": <b>Decrypted:</b> " + decryptedMessage;
                    
                    sendGroupMessage(formattedMessage, groupId);
                })
                .exceptionally(ex -> {
                    String errorTimestamp = dateFormat.format(new Date());
                    String errorMessage = "[" + errorTimestamp + "] " + displayName
                            + ": <b>Error decrypting message:</b> " + ex.getMessage();
                    
                    sendGroupMessage(errorMessage, groupId);
                    return null;
                });
        } catch (Exception e) {
            String timestamp = dateFormat.format(new Date());
            String errorMessage = "[" + timestamp + "] System: <b>Invalid Base64 encoding:</b> " + e.getMessage();
            
            sendGroupMessage(errorMessage, groupId);
        }
    }
private Aes256 initializeEncryption() {
    try {
        String keyString = "this-is-a-sample-encryption-key-123456";
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        
        if (keyBytes.length != 32) {
            byte[] adjustedKey = new byte[32];
            System.arraycopy(keyBytes, 0, adjustedKey, 0, Math.min(keyBytes.length, 32));
            return new Aes256(adjustedKey);
        } else {
            return new Aes256(keyBytes);
        }
    } catch (Exception e) {
        System.err.println("Failed to initialize encryption: " + e.getMessage());
        return null;
    }
}
}