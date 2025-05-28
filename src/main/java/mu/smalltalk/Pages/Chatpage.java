package mu.smalltalk.Pages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

import mu.smalltalk.Services.ChatService;
import mu.smalltalk.Services.EncryptionService;
import mu.smalltalk.Services.GlobalMessageBroadcaster;
import mu.smalltalk.Services.GroupService;
import mu.smalltalk.Services.MongoDbSerivce;
import mu.smalltalk.Services.UserService;
import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.Message;
import mu.smalltalk.entitis.User;
import mu.smalltalk.Repositories.MessageRepository;
import mu.smalltalk.security.Aes256;

@Route("chat")
public class Chatpage extends VerticalLayout implements BeforeEnterObserver {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MongoDbSerivce mongoDbService;
    
    @Autowired
    private ChatService chatService;  // הבעיה הייתה כאן - אין צורך בfinal ובאתחול ידני בבנאי

    private final EncryptionService encryptionService;
    
    private final MessageInput messageInput;
    private Upload mediaUpload;
    private final Aes256 aes;
    private final VerticalLayout chatContainer;
    private final VerticalLayout userListContainer;
    private static final int CHAT_HEIGHT = 500;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String sessionId;
    private Registration broadcasterRegistration;
    private ComboBox<Group> groupSelector;
    private String currentGroupId = null;

    private int lastSeenMessageCount = 0;
    
    // UI components for loading state
    private ProgressBar loadingProgressBar;
    private Div loadingMessageDiv;

    @Autowired  // הוספה של autowired לבנאי כדי להבטיח שהתלויות יוזרקו כראוי
    public Chatpage(ChatService chatService) {  // להזריק את chatService בבנאי במקום להסתמך על הזרקה בשדה
        this.chatService = chatService;  // שמירת ההפניה לשירות
        
        aes = initializeEncryption();
        encryptionService = new EncryptionService(aes);

        messageInput = new MessageInput();
        chatContainer = new VerticalLayout();
        userListContainer = new VerticalLayout();

        chatContainer.setWidthFull();
        chatContainer.setHeight(CHAT_HEIGHT, Unit.PIXELS);

        chatContainer.getStyle().set("overflow-y", "auto");
        chatContainer.getStyle().set("border", "1px solid #ddd");
        chatContainer.addClassName("chat-container");

        // Configure user list container
        userListContainer.setWidthFull();
        userListContainer.getStyle().set("padding", "10px");
        userListContainer.getStyle().set("margin-bottom", "10px");
        userListContainer.getStyle().set("border", "1px solid #ddd");
        userListContainer.getStyle().set("border-radius", "4px");

        // Initially hide user list container
        userListContainer.setVisible(false);
        
        // Create loading components
        loadingProgressBar = new ProgressBar();
        loadingProgressBar.setIndeterminate(true);
        loadingProgressBar.setWidth("100%");
        loadingProgressBar.setVisible(false);
        
        loadingMessageDiv = new Div(new Span("Loading messages..."));
        loadingMessageDiv.getStyle()
            .set("text-align", "center")
            .set("color", "#666")
            .set("padding", "10px");
        loadingMessageDiv.setVisible(false);

        // Create message input and upload layout in a custom way
        Div messageInputWrapper = createMessageInputWithUpload();

        // Initialize sessionId correctly - הזזנו את זה לאחרי האתחול של chatService
        sessionId = initializeSessionId();

        // Use authenticated user's email or name if available
        User currentUser = UserService.getAuthenticatedUser();
        String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;

        // Create group selector
        groupSelector = new ComboBox<>("Select Group");
        groupSelector.setItemLabelGenerator(Group::getName);
        groupSelector.setWidthFull();

        // Fill the group selector with the groups the user belongs to
        if (currentUser != null) {
            List<Group> userGroups = GroupService.getUserGroups(currentUser.getEmail());

            // If no groups exist, create a default group for this user
            if (userGroups.isEmpty()) {
                System.out.println("No groups found for user " + currentUser.getEmail() + ", creating a default group");
                Group defaultGroup = GroupService.createGroup("Default Group", currentUser.getEmail());
                userGroups = GroupService.getUserGroups(currentUser.getEmail());
            }

            groupSelector.setItems(userGroups);
        }

        groupSelector.addValueChangeListener(event -> {
            System.out.println("Selected Group: " + event.getValue());

            if (event.getValue() != null) {
                currentGroupId = event.getValue().getId();
                System.out.println("Current Group ID set to: " + currentGroupId);

                // Show user list when a group is selected
                userListContainer.setVisible(true);

                User loggedInUser = UserService.getAuthenticatedUser();
                String userName = (loggedInUser != null) ? loggedInUser.getFullName() : sessionId;

                // Update user activity status in chat service
                if (loggedInUser != null && chatService != null) { // חשוב: בדיקה שהשירות לא null
                    chatService.updateUserActivity(loggedInUser.getEmail());
                }

                // Just show the user name, not the group name
                updateSessionIdDisplay(userName);

                refreshChatHistory();
                updateUserList();
            } else {
                // Hide user list when no group is selected
                userListContainer.setVisible(false);
            }
        });
        
        // Create the navigation bar
        HorizontalLayout navbar = createNavigationBar();

        // Add a section for chat
        H3 chatHeader = new H3("Chat");

        // Add components to main layout in the correct order
        add(navbar, groupSelector, userListContainer, chatHeader, loadingProgressBar, 
            loadingMessageDiv, chatContainer, messageInputWrapper);

        setupMessageHandler();
        setupCrossBrowserCommunication();
        
        // Start a periodic task to update user online status every 30 seconds
        setupUserStatusUpdateTask();
    }

    private HorizontalLayout createNavigationBar() {
        HorizontalLayout navbar = new HorizontalLayout();
        navbar.setWidthFull();
        navbar.setHeight("64px");
        navbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        navbar.setAlignItems(Alignment.CENTER);
        navbar.getStyle()
            .set("background-color", "#ffffff")
            .set("padding", "0 24px")
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)")
            .set("position", "sticky")
            .set("top", "0")
            .set("z-index", "1000");
        
        // Logo container with image and text
        HorizontalLayout logoContainer = new HorizontalLayout();
        logoContainer.setAlignItems(Alignment.CENTER);
        logoContainer.setSpacing(true);
        logoContainer.getStyle().set("cursor", "pointer");
        
        // Logo image
        Image logo = new Image("images/live-chat.png", "SmallTalk");
        logo.setHeight("40px");
        logo.setWidth("auto");
        
        // Logo text
        H3 logoText = new H3("SmallTalk");
        logoText.getStyle()
            .set("margin", "0")
            .set("color", "#2a5885")
            .set("font-weight", "bold")
            .set("font-size", "22px");
        
        // Add logo and text to container
        logoContainer.add(logo, logoText);
        
        // Add click listener to navigate to home page
        logoContainer.addClickListener(e -> logoContainer.getUI().ifPresent(ui -> ui.navigate("")));
        
        // Navigation links with current username display
        HorizontalLayout navLinks = new HorizontalLayout();
        navLinks.setSpacing(true);
        navLinks.setAlignItems(Alignment.CENTER);
        
        Anchor homeLink = new Anchor("", "Home");
        Anchor chatLink = new Anchor("chat", "Chat");
        chatLink.getStyle().set("font-weight", "bold"); // Highlight current page
        Anchor featuresLink = new Anchor("#features", "Features");
        Anchor securityLink = new Anchor("#security", "Security");
        Anchor techLink = new Anchor("#technology", "Technology");
        Anchor aboutLink = new Anchor("#about", "About");
        
        // Get current user for display in navbar
        User currentUser = UserService.getAuthenticatedUser();
        
        // Add new group button to the navbar
        Button newGroupButton = new Button("Create New Group", new Icon(VaadinIcon.PLUS_CIRCLE));
        newGroupButton.addClickListener(e -> createNewGroup());
        newGroupButton.getStyle()
            .set("margin-left", "15px")
            .set("background-color", "#2a5885")
            .set("color", "white")
            .set("border-radius", "4px")
            .set("cursor", "pointer");
        
        // Authentication links - conditionally show based on authentication status
        if (currentUser != null) {
            // User is logged in, show username and logout
            
            // Create user display component
            Div userDiv = new Div();
            userDiv.getStyle()
                .set("margin-left", "20px")
                .set("font-weight", "bold")
                .set("display", "flex")
                .set("align-items", "center");
                
            // Add user icon
            Icon userIcon = VaadinIcon.USER.create();
            userIcon.getStyle()
                .set("margin-right", "8px")
                .set("color", "#2a5885");
                
            // Add username text
            Span usernameSpan = new Span(currentUser.getFullName());
            
            userDiv.add(userIcon, usernameSpan);
            
            // Create the logout link
            Anchor logoutLink = new Anchor("logout", "Logout");
            logoutLink.getStyle()
                .set("margin-left", "15px");
                
            navLinks.add(homeLink, chatLink, featuresLink, securityLink, techLink, aboutLink, newGroupButton, userDiv, logoutLink);
        } else {
            // User is not logged in, show login and signup
            Anchor loginLink = new Anchor("login", "Login");
            Anchor signupLink = new Anchor("signup", "Signup");
            navLinks.add(homeLink, chatLink, featuresLink, securityLink, techLink, aboutLink, loginLink, signupLink);
        }
        
        // Style all links
        for (int i = 0; i < navLinks.getComponentCount(); i++) {
            com.vaadin.flow.component.Component component = navLinks.getComponentAt(i);
            if (component instanceof Anchor) {
                ((Anchor) component).getStyle()
                    .set("color", "#444")
                    .set("text-decoration", "none")
                    .set("margin", "0 12px")
                    .set("padding", "6px 12px")
                    .set("border-radius", "4px")
                    .set("transition", "background-color 0.3s");
            }
        }
        
        // Add logo container and navigation links to navbar
        navbar.add(logoContainer, navLinks);
        return navbar;
    }

    private void addMessageToDatabase(String formattedMessage, String groupId) {
        if (groupId == null) {
            System.err.println("Cannot save message: Group ID is null");
            return;
        }

        User currentUser = UserService.getAuthenticatedUser();
        String senderId = (currentUser != null) ? currentUser.getEmail() : sessionId;
        
        // Update user activity in chat service
        if (currentUser != null && chatService != null) { // חשוב: בדיקה שהשירות לא null
            chatService.updateUserActivity(currentUser.getEmail());
        }

        // Add message to database using ChatService
        if (chatService != null) { // בדיקה שהשירות לא null
            chatService.addGroupMessage(formattedMessage, groupId, senderId);
        } else {
            System.err.println("Cannot save message: ChatService is null");
        }
    }

    private Div createMessageInputWithUpload() {
        // Main wrapper div that will contain both the input and upload button
        Div messageInputWrapper = new Div();
        messageInputWrapper.setWidthFull();
        messageInputWrapper.getStyle().set("display", "flex");
        messageInputWrapper.getStyle().set("align-items", "center");
        messageInputWrapper.getStyle().set("margin-top", "10px");
        messageInputWrapper.getStyle().set("background-color", "#f0f0f0");
        messageInputWrapper.getStyle().set("border-radius", "4px");
        messageInputWrapper.getStyle().set("padding", "0");

        // Style the message input to take most of the space but leave room for buttons
        messageInput.setWidth("75%");

        // Configure media upload with improved visibility
        MemoryBuffer buffer = new MemoryBuffer();
        mediaUpload = new Upload(buffer);
        configureMediaUpload(mediaUpload, buffer);

        // Create a container for the upload button to control its position and
        // appearance
        Div uploadButtonContainer = new Div(mediaUpload);
        uploadButtonContainer.setWidth("15%");
        uploadButtonContainer.getStyle().set("display", "flex");
        uploadButtonContainer.getStyle().set("align-items", "center");
        uploadButtonContainer.getStyle().set("justify-content", "center");
        uploadButtonContainer.getStyle().set("padding", "0 8px");

        // Add message input and upload button to the wrapper
        messageInputWrapper.add(messageInput, uploadButtonContainer);

        return messageInputWrapper;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!UserService.isUserAuthenticated()) {
            event.forwardTo("login");
        }

        initializeChatForAuthenticatedUser();
    }

    private void initializeChatForAuthenticatedUser() {
        User currentUser = UserService.getAuthenticatedUser();
        if (currentUser != null && chatService != null) { // חשוב: בדיקה שהשירות לא null
            // Update user activity status
            chatService.updateUserActivity(currentUser.getEmail());
            
            List<Group> userGroups = loadUserGroups(currentUser);

            setupUserInterface(currentUser, userGroups);
        }
    }

    private List<Group> loadUserGroups(User currentUser) {
        // Load groups from the GroupService
        return GroupService.getUserGroups(currentUser.getEmail());
    }

    private void setupUserInterface(User currentUser, List<Group> userGroups) {
        // Just update with the user's name, no group name
        updateSessionIdDisplay(currentUser.getFullName());

        if (groupSelector != null && !userGroups.isEmpty()) {
            groupSelector.setItems(userGroups);

            // Don't automatically select a group
            currentGroupId = null;
            groupSelector.setValue(null);

            // Initially hide user list since no group is selected
            userListContainer.setVisible(false);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registerWithBroadcaster();

        UI.getCurrent().access(() -> {
            // Update user online status on attach
            User currentUser = UserService.getAuthenticatedUser();
            if (currentUser != null && chatService != null) { // חשוב: בדיקה שהשירות לא null
                chatService.updateUserActivity(currentUser.getEmail());
            }
        });

        checkForNewMessages();

        UI ui = attachEvent.getUI();
        if (ui != null) {
            ui.setPollInterval(500);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
        
        // When detaching, we don't mark the user as offline immediately
        // This is handled by the timeout in ChatService
    }
    
    private void setupUserStatusUpdateTask() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.setPollInterval(30000); // 30 seconds
            
            // Set up a recurring task to update user status
            ui.addPollListener(event -> {
                User currentUser = UserService.getAuthenticatedUser();
                if (currentUser != null && chatService != null) { // חשוב: בדיקה שהשירות לא null
                    chatService.updateUserActivity(currentUser.getEmail());
                }
                
                // Update user list to refresh online statuses
                if (currentGroupId != null) {
                    updateUserList();
                }
            });
        }
    }

    private void loadExistingMessages() {
        if (currentGroupId == null || chatService == null) { // חשוב: בדיקה שהשירות לא null
            return;
        }
        
        // Show loading indicators
        showLoadingIndicators(true);
        
        // Clear the chat container
        chatContainer.removeAll();
        
        // Use CompletableFuture to load messages asynchronously
        chatService.loadMessagesAsync(currentGroupId)
            .thenAccept(existingMessages -> {
                UI ui = UI.getCurrent();
                if (ui != null && ui.isAttached()) {
                    ui.access(() -> {
                        for (String message : existingMessages) {
                            addMessageToChat(message);
                        }
                        lastSeenMessageCount = existingMessages.size();
                        
                        // Hide loading indicators
                        showLoadingIndicators(false);
                        
                        scrollToBottom();
                        ui.push();
                    });
                }
            })
            .exceptionally(ex -> {
                UI ui = UI.getCurrent();
                if (ui != null && ui.isAttached()) {
                    ui.access(() -> {
                        showLoadingIndicators(false);
                        
                        Notification.show("Error loading messages: " + ex.getMessage(),
                                3000, Notification.Position.MIDDLE);
                        ui.push();
                    });
                }
                return null;
            });
    }
    
    private void showLoadingIndicators(boolean show) {
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                loadingProgressBar.setVisible(show);
                loadingMessageDiv.setVisible(show);
                ui.push();
            });
        }
    }

    private void refreshChatHistory() {
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                if (currentGroupId != null) {
                    loadExistingMessages();
                    updateUserList();
                    ui.push();
                }
            });
        }
    }

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
            
            // Update user activity status
            if (currentUser != null && chatService != null) { // חשוב: בדיקה שהשירות לא null
                chatService.updateUserActivity(currentUser.getEmail());
            }

            System.out.println("Received message from " + displayName + " in group " + currentGroupId + ": " + message);

            String formattedMessage = "[" + timestamp + "] " + displayName + ":<br>" + message;
            sendGroupMessage(formattedMessage, currentGroupId);

            encryptionService.encryptStringAsync(message)
                    .thenAccept(encryptedMessage -> {
                        if (currentUI.isAttached() && !currentSession.getSession().isNew()) {
                            currentSession.lock();
                            try {
                                currentUI.access(() -> {
                                    String encTimestamp = dateFormat.format(new Date());
                                    String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessage);

                                    String encryptedFormattedMessage = "[" + encTimestamp + "] " + displayName +
                                            " <b>[Encrypted]</b>:<br>" + encodedMessage;

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
                                    String errorMessage = "[" + errorTimestamp + "] " + displayName +
                                            " <b>[Error]</b>:<br>Error encrypting message: " + ex.getMessage();

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

    public void handleChatUpdate(String timestamp) {
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                checkForNewMessages();
                if (currentGroupId != null) {
                    updateUserList();
                }
                ui.push();
            });
        }
    }

    public void checkForNewMessages() {
        if (currentGroupId == null || chatService == null) { // חשוב: בדיקה שהשירות לא null
            return;
        }

        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                List<String> allMessages = chatService.getGroupMessages(currentGroupId);
                int currentMessageCount = allMessages.size();

                if (currentMessageCount > lastSeenMessageCount) {
                    for (int i = lastSeenMessageCount; i < currentMessageCount; i++) {
                        addMessageToChat(allMessages.get(i));
                    }
                    lastSeenMessageCount = currentMessageCount;

                    scrollToBottom();

                    ui.getPage().executeJs("window.notifyOtherWindows();");

                    ui.push();
                }
            });
        }
    }

    private void registerWithBroadcaster() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            if (broadcasterRegistration != null) {
                broadcasterRegistration.remove();
                broadcasterRegistration = null;
            }

            System.out.println("Registering UI: " + ui.getUIId() + " with broadcaster");

            // Using the GroupMessageConsumer interface
            broadcasterRegistration = GlobalMessageBroadcaster.register(ui, (message, groupId) -> {
                if (ui.isAttached() && (currentGroupId != null && currentGroupId.equals(groupId))) {
                    ui.access(() -> {
                        addMessageToChat(message);

                        scrollToBottom();
                        ui.push();
                    });
                }
            });

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

        checkForNewMessages();
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
            
            // Update user activity in chat service
            if (chatService != null) { // חשוב: בדיקה שהשירות לא null
                chatService.updateUserActivity(currentUser.getEmail());
            }

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
        // Update just the user's name without group name
        getChildren().forEach(component -> {
            if (component instanceof H2) {
                ((H2) component).setText(id);
            }
        });
    }

   private void configureMediaUpload(Upload upload, MemoryBuffer buffer) {
        // Create an upload button with an icon and text for better clarity
        Button uploadButton = new Button("Upload", new Icon(VaadinIcon.UPLOAD));
        uploadButton.getElement().setAttribute("aria-label", "Upload media");
        uploadButton.getStyle().set("cursor", "pointer");
        uploadButton.getStyle().set("background-color", "#e0e0e0");
        uploadButton.getStyle().set("border", "1px solid #ccc");
        uploadButton.getStyle().set("border-radius", "4px");
        uploadButton.getStyle().set("padding", "6px 12px");
        uploadButton.getStyle().set("color", "#333");
        uploadButton.getStyle().set("font-weight", "normal");
        uploadButton.getStyle().set("margin", "0 4px");

        // Add hover effect for better UX
        uploadButton.getElement().addEventListener("mouseover", event -> {
            uploadButton.getStyle().set("background-color", "#d0d0d0");
        });

        uploadButton.getElement().addEventListener("mouseout", event -> {
            uploadButton.getStyle().set("background-color", "#e0e0e0");
        });

        // Set the button as the upload button
        upload.setUploadButton(uploadButton);

        // Hide all the default upload components - we only want our custom button
        upload.getElement().getStyle().set("display", "inline-block");
        upload.getElement().getStyle().set("margin", "0");
        upload.getElement().getStyle().set("padding", "0");
        upload.getElement().getStyle().set("background", "transparent");
        upload.getElement().getStyle().set("min-height", "auto");

        // Only show the button, hide everything else
        upload.getElement().executeJs(
                "this.querySelector('vaadin-upload-file-list').style.display = 'none';" +
                        "this.shadowRoot.querySelector('[part=\"drop-label\"]').style.display = 'none';");

        upload.setAcceptedFileTypes("image/*", "audio/*");
        upload.setMaxFileSize(16 * 1024 * 1024); // 16 MB

        // Add tooltip to explain what the button does
       Tooltip.forComponent(uploadButton).withText("Upload an image or audio")
                .withPosition(Tooltip.TooltipPosition.TOP);

        // Add handlers for upload events
        upload.addSucceededListener(event -> {
            if (currentGroupId == null) {
                Notification.show("selcte group", 2000, Notification.Position.MIDDLE);
                return;
            }

            UI ui = UI.getCurrent();
            VaadinSession session = VaadinSession.getCurrent();

            User currentUser = UserService.getAuthenticatedUser();
            String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;
            String senderId = (currentUser != null) ? currentUser.getEmail() : sessionId;

            String fileName = event.getFileName();
            String mimeType = event.getMIMEType();
            try {
                InputStream inputStream = buffer.getInputStream();
                byte[] fileData = inputStream.readAllBytes();
                String base64Data = Base64.getEncoder().encodeToString(fileData);
                String timestamp = dateFormat.format(new Date());

                String mediaType = mimeType.startsWith("image/") ? "IMAGE" : "SOUND";
                Message mediaMessage = new Message(senderId, currentGroupId, null, fileData, mediaType, currentGroupId);
                messageRepository.save(mediaMessage);

                if (mimeType.startsWith("image/")) {
                    String imageHtml = "[" + timestamp + "] " + displayName + " <b>[תמונה]</b>:<br>" +
                            "<img src='data:" + mimeType + ";base64," + base64Data +
                            "' alt='Image' style='max-width: 100%; max-height: 300px;'>";
                    sendGroupMessage(imageHtml, currentGroupId);
                } else if (mimeType.startsWith("audio/")) {
                    String audioHtml = "[" + timestamp + "] " + displayName + " <b>[אודיו]</b>:<br>" +
                            "<audio controls><source src='data:" + mimeType + ";base64," +
                            base64Data + "' type='" + mimeType + "'></audio>";
                    sendGroupMessage(audioHtml, currentGroupId);
                }

                final byte[] finalFileData = fileData;
                encryptionService.encryptAsync(finalFileData)
                        .thenAccept(encryptedData -> {
                            if (ui.isAttached() && !session.getSession().isNew()) {
                                session.lock();
                                try {
                                    ui.access(() -> {
                                        String encTimestamp = dateFormat.format(new Date());
                                        String successMessage = "[" + encTimestamp + "] " + displayName +
                                                " <b>[File Encrypted]</b>:<br>The file " + fileName
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
                                        String errorMessage = "[" + errorTimestamp + "] " + displayName +
                                                " <b>[Error]</b>:<br>Error encrypting file: " + ex.getMessage();

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
                String errorMessage = "[" + errorTimestamp + "] " + displayName +
                        " <b>[Error]</b>:<br>Error processing file: " + e.getMessage();

                sendGroupMessage(errorMessage, currentGroupId);
            }

            // IMPORTANT: Clear the upload component to allow new uploads
            upload.getElement().executeJs("this.files = []");
        });

        // Handle the file reject event
        upload.addFileRejectedListener(event -> {
            String errorMessage = event.getErrorMessage();
            Notification.show("File rejected: " + errorMessage, 3000, Notification.Position.MIDDLE);

            // Also clear the upload component after rejection
            upload.getElement().executeJs("this.files = []");
        });
    }

    private void sendGroupMessage(String message, String groupId) {
        if (groupId == null) {
            System.err.println("Cannot send message: Group ID is null");
            return;
        }

        addMessageToDatabase(message, groupId);

        GlobalMessageBroadcaster.broadcastToGroup(message, groupId);

        UI.getCurrent().getPage().executeJs("window.notifyOtherWindows();");
    }

    private void addMessageToChat(String message) {
        Div messageDiv = new Div();
        messageDiv.getElement().setProperty("innerHTML", message);

        messageDiv.getStyle().set("background-color", "#f0f0f0");
        messageDiv.getStyle().set("color", "#333");
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

            UI ui = UI.getCurrent();
            VaadinSession session = VaadinSession.getCurrent();

            User currentUser = UserService.getAuthenticatedUser();
            String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;

            encryptionService.decryptAsync(encryptedBytes)
                    .thenAccept(decryptedBytes -> {
                        if (ui.isAttached() && !session.getSession().isNew()) {
                            session.lock();
                            try {
                                ui.access(() -> {
                                    String decTimestamp = dateFormat.format(new Date());
                                    String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);
                                    String decryptedMessage = "[" + decTimestamp + "] " + displayName +
                                            " <b>[Decrypted]</b>:<br>" + decryptedText;

                                    sendGroupMessage(decryptedMessage, groupId);

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
                                    String errorMessage = "[" + errorTimestamp + "] " + displayName +
                                            " <b>[Error]</b>:<br>Error decrypting message: " + ex.getMessage();

                                    sendGroupMessage(errorMessage, groupId);

                                    ui.push();
                                });
                            } finally {
                                session.unlock();
                            }
                        }
                        return null;
                    });
        } catch (IllegalArgumentException e) {
            String errorTimestamp = dateFormat.format(new Date());
            User currentUser = UserService.getAuthenticatedUser();
            String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;

            String errorMessage = "[" + errorTimestamp + "] " + displayName +
                    " <b>[Error]</b>:<br>Error decoding Base64: " + e.getMessage();

            sendGroupMessage(errorMessage, groupId);
        }
    }

    private Aes256 initializeEncryption() {
        try {
            byte[] key = new byte[32];

            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) i;
            }

            return new Aes256(key);
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error initializing encryption: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE);
            return null;
        }
    }

   private void updateUserList() {
    if (currentGroupId == null) {
        userListContainer.setVisible(false);
        return;
    }

    userListContainer.setVisible(true);

    // Clear previous list
    userListContainer.removeAll();

    // Get group members
    Group currentGroup = GroupService.getGroupById(currentGroupId);
    if (currentGroup == null) {
        userListContainer.add(new Span("No members in this group"));
        return;
    }

    // Get the list of user emails/IDs
    List<String> memberEmails = currentGroup.getUsers();

    if (memberEmails == null || memberEmails.isEmpty()) {
        userListContainer.add(new Span("No members in this group"));
        return;
    }

    // Create a horizontal layout with space-between for the header and members
    HorizontalLayout headerAndMembersLayout = new HorizontalLayout();
    headerAndMembersLayout.setWidthFull();
    headerAndMembersLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    headerAndMembersLayout.setAlignItems(Alignment.CENTER);

    // Add the Group Members header to the left
    H3 groupMembersHeader = new H3("Group Members");
    groupMembersHeader.getStyle().set("margin", "0");
    
    // Create the members container to the right
    HorizontalLayout membersLayout = new HorizontalLayout();
    membersLayout.getStyle().set("flex-wrap", "wrap");
    membersLayout.setAlignItems(Alignment.CENTER);

    // Add members to the list with online status indicators
    for (String memberEmail : memberEmails) {
        User member = UserService.getUserByEmail(memberEmail);
        if (member != null) {
            Div memberDiv = new Div();

            memberDiv.getStyle()
                .set("background-color", "#e4e4e4")
                .set("color", "#333")
                .set("padding", "6px 12px")
                .set("margin", "4px")
                .set("border-radius", "16px")
                .set("display", "flex")
                .set("align-items", "center");

            // Add an online status indicator (green dot for online, gray for offline)
            // This is a mock implementation - you would need actual online status tracking
            boolean isOnline = Math.random() > 0.5; // Mock status - replace with actual status check
            
            Div statusDot = new Div();
            statusDot.getStyle()
                .set("width", "8px")
                .set("height", "8px")
                .set("border-radius", "50%")
                .set("background-color", isOnline ? "#4CAF50" : "#9E9E9E")
                .set("margin-right", "6px");
            
            // Add a user icon
            Icon userIcon = VaadinIcon.USER.create();
            userIcon.getStyle().set("margin-right", "6px");

            Span nameSpan = new Span(member.getFullName());
            memberDiv.add(statusDot, userIcon, nameSpan);

            membersLayout.add(memberDiv);
        }
    }

    // Add both components to the layout
    headerAndMembersLayout.add(groupMembersHeader, membersLayout);
    userListContainer.add(headerAndMembersLayout);
}

    private void createNewGroup() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        H3 title = new H3("Create New Group");

        // Group name field
        TextField groupNameField = new TextField("Group Name");
        groupNameField.setWidthFull();
        groupNameField.setRequired(true);

        // Create a multi-select combo box for users
        List<User> allUsers = UserService.getAllUsers();

        MultiSelectComboBox<User> userSelector = new MultiSelectComboBox<>("Add Members");
        userSelector.setItems(allUsers);
        userSelector.setItemLabelGenerator(User::getFullName);
        userSelector.setWidthFull();

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button createButton = new Button("Create", e -> {
            String groupName = groupNameField.getValue().trim();
            if (groupName.isEmpty()) {
                Notification.show("Please enter a group name", 2000, Notification.Position.MIDDLE);
                return;
            }

            User currentUser = UserService.getAuthenticatedUser();
            if (currentUser == null) {
                Notification.show("You must be logged in to create a group", 2000, Notification.Position.MIDDLE);
                dialog.close();
                return;
            }

            // Create the new group
            Group newGroup = GroupService.createGroup(groupName, currentUser.getEmail());

            // Add selected users
            Set<User> selectedUsers = userSelector.getValue();
            for (User user : selectedUsers) {
                GroupService.addUserToGroup(user.getEmail(), newGroup.getId());
            }

            // Update the group selector
            updateGroupSelector();

            // Select the new group
            if (newGroup != null) {
                groupSelector.setValue(newGroup);
                currentGroupId = newGroup.getId();
                loadExistingMessages();
                updateUserList();
            }

            Notification.show("Group created: " + groupName, 2000, Notification.Position.BOTTOM_CENTER);
            dialog.close();
        });

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, createButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setWidthFull();

        dialogLayout.add(title, groupNameField, userSelector, buttonLayout);
        dialog.add(dialogLayout);
        dialog.open();
    }

    private void updateGroupSelector() {
        User currentUser = UserService.getAuthenticatedUser();
        if (currentUser != null) {
            List<Group> userGroups = GroupService.getUserGroups(currentUser.getEmail());
            groupSelector.setItems(userGroups);
        }
    }
}