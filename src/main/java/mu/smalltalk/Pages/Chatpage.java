package mu.smalltalk.Pages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
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
import mu.smalltalk.Repositories.MessageRepository;
import mu.smalltalk.Services.ChatService;
import mu.smalltalk.Services.EncryptionService;
import mu.smalltalk.Services.GlobalMessageBroadcaster;
import mu.smalltalk.Services.GroupService;
import mu.smalltalk.Services.MongoDbSerivce;
import mu.smalltalk.Services.UserService;
import mu.smalltalk.entitis.Group;
import mu.smalltalk.entitis.Message;
import mu.smalltalk.entitis.User;
import mu.smalltalk.security.Aes256;

@Route("chat")
public class Chatpage extends VerticalLayout implements BeforeEnterObserver {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MongoDbSerivce mongoDbService;

    @Autowired
    private ChatService chatService;

    private final EncryptionService encryptionService;

    private final MessageInput messageInput;
    private Upload mediaUpload;
    private final Aes256 aes;
    private final VerticalLayout chatContainer;
    private final VerticalLayout userListContainer;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String sessionId;
    private Registration broadcasterRegistration;
    private ComboBox<Group> groupSelector;
    private String currentGroupId = null;

    private int lastSeenMessageCount = 0;

    // UI components for loading state
    private ProgressBar loadingProgressBar;
    private Div loadingMessageDiv;

    // New components for enhanced functionality
    private Button toggleEncryptionButton;
    private boolean showEncryptionMessages = true;
    private Div messageStatusDiv;

    @Autowired
    public Chatpage(ChatService chatService) {
        this.chatService = chatService;

        aes = initializeEncryption();
        encryptionService = new EncryptionService(aes);

        messageInput = new MessageInput();
        chatContainer = new VerticalLayout();
        userListContainer = new VerticalLayout();

        // Make the entire layout take full height
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Configure chat container for full size
        chatContainer.setSizeFull();
        chatContainer.getStyle().set("overflow-y", "auto");
        chatContainer.getStyle().set("border", "1px solid #ddd");
        chatContainer.addClassName("chat-container");
        chatContainer.getStyle().set("flex-grow", "1");

        // Configure user list container
        userListContainer.setWidthFull();
        userListContainer.getStyle().set("padding", "10px");
        userListContainer.getStyle().set("margin-bottom", "10px");
        userListContainer.getStyle().set("border", "1px solid #ddd");
        userListContainer.getStyle().set("border-radius", "4px");
        userListContainer.getStyle().set("flex-shrink", "0");

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

        // Create message status div
        messageStatusDiv = new Div();
        messageStatusDiv.setVisible(false);
        messageStatusDiv.getStyle()
                .set("background-color", "#e8f5e8")
                .set("color", "#2d5a2d")
                .set("padding", "8px")
                .set("margin", "5px 0")
                .set("border-radius", "4px")
                .set("text-align", "center");

        // Create toggle encryption button
        toggleEncryptionButton = new Button("Hide Encryption", new Icon(VaadinIcon.EYE_SLASH));
        toggleEncryptionButton.addClickListener(e -> toggleEncryptionDisplay());
        toggleEncryptionButton.getStyle()
                .set("margin-bottom", "10px")
                .set("background-color", "#f0f0f0")
                .set("border", "1px solid #ccc");

        // Create message input and upload layout
        Div messageInputWrapper = createMessageInputWithUpload();

        // Initialize sessionId
        sessionId = initializeSessionId();

        // Use authenticated user's email or name if available
        User currentUser = UserService.getAuthenticatedUser();
        // String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;

        // Create group selector
        groupSelector = new ComboBox<>("Select Group");
        groupSelector.setItemLabelGenerator(Group::getName);
        groupSelector.setWidthFull();
        groupSelector.getStyle().set("flex-shrink", "0");

        // Fill the group selector with the groups the user belongs to
        if (currentUser != null) {
            List<Group> userGroups = GroupService.getUserGroups(currentUser.getEmail());

            // If no groups exist, create a default group for this user
            if (userGroups.isEmpty()) {
                // System.out.println("No groups found for user " + currentUser.getEmail() + ",
                // creating a default group");
                Group defaultGroup = GroupService.createGroup("Default Group", currentUser.getEmail());
                userGroups = GroupService.getUserGroups(currentUser.getEmail());
            }

            groupSelector.setItems(userGroups);
        }

        groupSelector.addValueChangeListener(event -> {
            // System.out.println("Selected Group: " + event.getValue());

            if (event.getValue() != null) {
                currentGroupId = event.getValue().getId();
                // System.out.println("Current Group ID set to: " + currentGroupId);

                // Show user list when a group is selected
                userListContainer.setVisible(true);
                toggleEncryptionButton.setVisible(true);

                User loggedInUser = UserService.getAuthenticatedUser();
                String userName = (loggedInUser != null) ? loggedInUser.getFullName() : sessionId;

                // Just show the user name, not the group name
                updateSessionIdDisplay(userName);

                refreshChatHistory();
                updateUserList();

                // Enable message input when group is selected
                messageInput.setEnabled(true);
                if (mediaUpload != null) {
                    mediaUpload.setVisible(true);
                }
            } else {
                // Hide user list when no group is selected
                userListContainer.setVisible(false);
                toggleEncryptionButton.setVisible(false);

                // Disable message input when no group is selected
                messageInput.setEnabled(false);
                if (mediaUpload != null) {
                    mediaUpload.setVisible(false);
                }
            }
        });

        // Create the navigation bar
        HorizontalLayout navbar = createNavigationBar();

        // Add a section for chat
        H3 chatHeader = new H3("Chat");
        chatHeader.getStyle().set("flex-shrink", "0");

        // Initially disable message input until group is selected
        messageInput.setEnabled(false);

        // Add components to main layout in the correct order
        add(navbar, groupSelector, toggleEncryptionButton, userListContainer, chatHeader,
                loadingProgressBar, loadingMessageDiv, messageStatusDiv, chatContainer, messageInputWrapper);

        // Set flex properties to make chat container take remaining space
        setFlexGrow(1, chatContainer);

        setupMessageHandler();
        setupCrossBrowserCommunication();

        // Initially hide encryption toggle button
        toggleEncryptionButton.setVisible(false);
    }

    private void toggleEncryptionDisplay() {
        showEncryptionMessages = !showEncryptionMessages;

        if (showEncryptionMessages) {
            toggleEncryptionButton.setText("Hide Encryption");
            toggleEncryptionButton.setIcon(new Icon(VaadinIcon.EYE_SLASH));
        } else {
            toggleEncryptionButton.setText("Show Encryption");
            toggleEncryptionButton.setIcon(new Icon(VaadinIcon.EYE));
        }

        // Refresh chat to apply filter
        refreshChatHistory();
    }

    private void showMessageStatus(String status, boolean isSuccess) {
        messageStatusDiv.removeAll();
        messageStatusDiv.add(new Span(status));

        if (isSuccess) {
            messageStatusDiv.getStyle()
                    .set("background-color", "#e8f5e8")
                    .set("color", "#2d5a2d");
        } else {
            messageStatusDiv.getStyle()
                    .set("background-color", "#ffeaea")
                    .set("color", "#d32f2f");
        }

        messageStatusDiv.setVisible(true);

        // Auto-hide after 3 seconds
        UI.getCurrent().getPage().executeJs(
                "setTimeout(() => { " +
                        "  const statusDiv = document.querySelector('.message-status');" +
                        "  if (statusDiv) statusDiv.style.display = 'none';" +
                        "}, 3000);");
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
                .set("z-index", "1000")
                .set("flex-shrink", "0");

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

            navLinks.add(homeLink, chatLink, featuresLink, securityLink, techLink, aboutLink, newGroupButton, userDiv,
                    logoutLink);
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
            // System.err.println("Cannot save message: Group ID is null");
            return;
        }

        User currentUser = UserService.getAuthenticatedUser();
        String senderId = (currentUser != null) ? currentUser.getEmail() : sessionId;

        // Add message to database using ChatService
        if (chatService != null) {
            chatService.addGroupMessage(formattedMessage, groupId, senderId);
        } else {
            // System.err.println("Cannot save message: ChatService is null");
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
        messageInputWrapper.getStyle().set("flex-shrink", "0");

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
        // Initially hide media upload
        mediaUpload.setVisible(false);
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
        if (currentUser != null) {
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
        checkForNewMessages();

        UI ui = attachEvent.getUI();
        if (ui != null) {
            // Reduce polling frequency for better performance
            ui.setPollInterval(1000); // Changed from 500ms to 1000ms
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

    private void loadExistingMessages() {
        if (currentGroupId == null || chatService == null) {
            return;
        }
        // Show minimal loading indicator
        showLoadingIndicators(true);
        // Clear chat container
        chatContainer.removeAll();
        // Load messages with improved performance
        chatService.loadMessagesAsync(currentGroupId)
                .thenAccept(existingMessages -> {
                    UI ui = UI.getCurrent();
                    if (ui != null && ui.isAttached()) {
                        ui.access(() -> {
                            try {
                                // Filter messages based on encryption display setting
                                List<String> filteredMessages = filterMessages(existingMessages);

                                // Add messages in batches for smoother UI
                                addMessagesInBatches(filteredMessages);
                                lastSeenMessageCount = existingMessages.size();

                                // Hide loading indicators quickly
                                showLoadingIndicators(false);

                                // Scroll to bottom after short delay
                                UI.getCurrent().getPage().executeJs(
                                        "setTimeout(() => { " +
                                                "  const chatContainer = document.querySelector('.chat-container');" +
                                                "  if (chatContainer) chatContainer.scrollTop = chatContainer.scrollHeight;"
                                                +
                                                "}, 50);");

                            } catch (Exception e) {
                                // System.err.println("Error displaying messages: " + e.getMessage());
                                showLoadingIndicators(false);
                            }
                            ui.push();
                        });
                    }
                })
                .exceptionally(ex -> {
                    UI ui = UI.getCurrent();
                    if (ui != null && ui.isAttached()) {
                        ui.access(() -> {
                            showLoadingIndicators(false);
                            // Don't show error to user, just log it
                            // System.err.println("Error loading messages: " + ex.getMessage());
                            ui.push();
                        });
                    }
                    return null;
                });
    }

    private List<String> filterMessages(List<String> messages) {
        if (showEncryptionMessages) {
            return messages;
        }
        // Filter out encryption-related messages
        return messages.stream()
                .filter(message -> !message.contains("[Encrypted]") &&
                        !message.contains("[Decrypted]") &&
                        !message.contains("[File Encrypted]"))
                .collect(Collectors.toList());
    }

    private void addMessagesInBatches(List<String> messages) {
        if (messages.isEmpty())
            return;

        final int BATCH_SIZE = 10;
        final int DELAY_MS = 10;

        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            final int startIndex = i;
            final int endIndex = Math.min(i + BATCH_SIZE, messages.size());
            final List<String> batch = messages.subList(startIndex, endIndex);

            // Add batch with small delay for smooth rendering
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => { window.addMessageBatch && window.addMessageBatch(); }, "
                            + (i * DELAY_MS / BATCH_SIZE) + ");");

            // Add messages immediately (the timeout is just for smooth scrolling)
            for (String message : batch) {
                addMessageToChat(message);
            }
        }
    }

    private void addMessageToChat(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        // Filter encryption messages if setting is disabled
        if (!showEncryptionMessages &&
                (message.contains("[Encrypted]") ||
                        message.contains("[Decrypted]") ||
                        message.contains("[File Encrypted]"))) {
            return;
        }
        Div messageDiv = new Div();
        messageDiv.getElement().setProperty("innerHTML", message);
        // Optimized styling
        messageDiv.addClassName("chat-message");
        messageDiv.getStyle()
                .set("background-color", "#f8f9fa")
                .set("color", "#333")
                .set("padding", "8px 12px")
                .set("margin-bottom", "6px")
                .set("border-radius", "8px")
                .set("word-wrap", "break-word")
                .set("max-width", "100%")
                .set("animation", "fadeIn 0.3s ease-in");

        chatContainer.add(messageDiv);

        // Limit number of messages in DOM for performance
        if (chatContainer.getComponentCount() > 100) {
            com.vaadin.flow.component.Component firstComponent = chatContainer.getComponentAt(0);
            chatContainer.remove(firstComponent);
        }
    }

    private void showLoadingIndicators(boolean show) {
        // Simplified loading indicator
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisible(show);
        }
        if (loadingMessageDiv != null) {
            loadingMessageDiv.setVisible(show && chatContainer.getComponentCount() == 0);
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

            // 1. שלח את ההודעה הרגילה מיד (זה יוסיף אותה גם לצ'אט המקומי)
            String normalMessage = "[" + timestamp + "] " + displayName + ":<br>" + message;
            sendGroupMessageSafely(normalMessage, currentGroupId);

            // 2. הצג הודעת הצפנה (ללא שמירה בבסיס נתונים - רק הצגה מקומית)
            String encryptingMessage = "[" + timestamp + "] " + displayName +
                    " <b>[Encrypting...]</b>:<br>🔐 Encrypting message...";

            // הוסף מיד לצ'אט המקומי (ללא שמירה בבסיס נתונים)
            addMessageToChat(encryptingMessage);

            // 3. בצע הצפנה ברקע
            encryptionService.encryptStringAsync(message)
                    .thenAccept(encryptedMessage -> {
                        if (currentUI.isAttached() && !currentSession.getSession().isNew()) {
                            currentSession.lock();
                            try {
                                currentUI.access(() -> {
                                    String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessage);
                                    String encTimestamp = dateFormat.format(new Date());

                                    // החלף את הודעת ה"Encrypting..." בהודעה המוצפנת
                                    String encryptedDisplayMessage = "[" + encTimestamp + "] " + displayName +
                                            " <b>[Encrypted]</b>:<br>" + encodedMessage;

                                    // עדכן את ההודעה בצ'אט (החלף את הקודמת)
                                    replaceLastEncryptingMessage(encryptedDisplayMessage);

                                    // שמור בבסיס נתונים ושלח לאחרים
                                    addMessageToDatabase(encryptedDisplayMessage, currentGroupId);
                                    GlobalMessageBroadcaster.broadcastToGroup(encryptedDisplayMessage, currentGroupId);
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
                                    String errorMessage = "[" + timestamp + "] " + displayName +
                                            " <b>[Encryption Failed]</b>:<br>❌ " + ex.getMessage();

                                    // החלף את הודעת ה"Encrypting..." בהודעת שגיאה
                                    replaceLastEncryptingMessage(errorMessage);
                                    currentUI.push();
                                });
                            } finally {
                                currentSession.unlock();
                            }
                        }
                        return null;
                    });
            showMessageStatus("Message sent successfully!", true);
        });
    }

    private void replaceLastEncryptingMessage(String newMessage) {
        // מצא את ההודעה האחרונה עם "Encrypting..."
        for (int i = chatContainer.getComponentCount() - 1; i >= 0; i--) {
            com.vaadin.flow.component.Component component = chatContainer.getComponentAt(i);
            if (component instanceof Div) {
                Div messageDiv = (Div) component;
                String innerHTML = messageDiv.getElement().getProperty("innerHTML");
                if (innerHTML != null && innerHTML.contains("[Encrypting...]")) {
                    // החלף את התוכן
                    messageDiv.getElement().setProperty("innerHTML", newMessage);

                    // עדכן סטיילינג להודעה מוצלחת
                    messageDiv.getStyle()
                            .set("background-color", "#e8f5e8")
                            .set("border-left", "4px solid #4caf50");

                    return;
                }
            }
        }
        // אם לא נמצאה ההודעה, פשוט הוסף חדשה
        addMessageToChat(newMessage);
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
        if (currentGroupId == null || chatService == null) {
            return;
        }
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                try {
                    List<String> allMessages = chatService.getGroupMessages(currentGroupId);
                    int currentMessageCount = allMessages.size();

                    if (currentMessageCount > lastSeenMessageCount) {
                        // Add only new messages
                        for (int i = lastSeenMessageCount; i < currentMessageCount; i++) {
                            addMessageToChat(allMessages.get(i));
                        }
                        lastSeenMessageCount = currentMessageCount;

                        // Smooth scroll to bottom
                        UI.getCurrent().getPage().executeJs(
                                "setTimeout(() => { " +
                                        "  const chatContainer = document.querySelector('.chat-container');" +
                                        "  if (chatContainer) {" +
                                        "    chatContainer.scrollTo({" +
                                        "      top: chatContainer.scrollHeight," +
                                        "      behavior: 'smooth'" +
                                        "    });" +
                                        "  }" +
                                        "}, 100);");

                        ui.getPage().executeJs("window.notifyOtherWindows && window.notifyOtherWindows();");
                    }
                } catch (Exception e) {
                    // System.err.println("Error checking messages: " + e.getMessage());
                }
                ui.push();
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

            // System.out.println("Registering UI: " + ui.getUIId() + " with broadcaster");

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
                // System.err.println("Failed to register with broadcaster for UI: " +
                // ui.getUIId());
                Notification.show("Failed to connect to the chat server. Please refresh the page.",
                        3000, Notification.Position.MIDDLE);
            }
        } else {
            // System.err.println("Cannot register with broadcaster - UI is null");
        }
    }

    public void ensureRegistration() {
        if (broadcasterRegistration == null || !GlobalMessageBroadcaster.isRegistered(UI.getCurrent())) {
            // System.out.println("Re-registering with broadcaster after reconnection");
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

        // Helper method to clear upload component completely
        Runnable clearUpload = () -> {
            upload.getElement().executeJs(
                    "this.files = []; " +
                            "this.shadowRoot.querySelector('input[type=\"file\"]').value = ''; " +
                            "this.requestUpdate();");
            upload.clearFileList();
            // Force button to be enabled again
            uploadButton.setEnabled(true);
        };

       upload.addSucceededListener(event -> {
    if (currentGroupId == null) {
        Notification.show("Please select a group first", 2000, Notification.Position.MIDDLE);
        clearUpload.run();
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

        String mediaHtml;
        if (mimeType.startsWith("image/")) {
            mediaHtml = "[" + timestamp + "] " + displayName + " <b>[תמונה]</b>:<br>" +
                    "<img src='data:" + mimeType + ";base64," + base64Data +
                    "' alt='Image' style='max-width: 100%; max-height: 300px;'>";
        } else if (mimeType.startsWith("audio/")) {
            mediaHtml = "[" + timestamp + "] " + displayName + " <b>[אודיו]</b>:<br>" +
                    "<audio controls><source src='data:" + mimeType + ";base64," +
                    base64Data + "' type='" + mimeType + "'></audio>";
        } else {
            clearUpload.run();
            return;
        }

        // **תיקון**: שלח את ההודעה רק פעם אחת דרך sendGroupMessageSafely
        // הפונקציה הזו כבר מטפלת בהוספה מקומית, שמירה ושליחה לאחרים
        sendGroupMessageSafely(mediaHtml, currentGroupId);

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

                                // שלח גם את הודעת ההצפנה דרך sendGroupMessageSafely
                                sendGroupMessageSafely(successMessage, currentGroupId);
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

                                // שלח גם את הודעת השגיאה דרך sendGroupMessageSafely
                                sendGroupMessageSafely(errorMessage, currentGroupId);
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

        // שלח גם את הודעת השגיאה דרך sendGroupMessageSafely
        sendGroupMessageSafely(errorMessage, currentGroupId);
    }

    clearUpload.run();
});
    }

    private void sendGroupMessageSafely(String message, String groupId) {
        if (groupId == null) {
            return;
        }

        // הוסף את ההודעה מיד לצ'אט המקומי של השולח
        addMessageToChat(message);

        // שמור בבסיס נתונים
        addMessageToDatabase(message, groupId);

        // שלח למשתמשים אחרים (ללא הוספה מקומית אצלם כי זה כבר נעשה בaddMessageToChat)
        GlobalMessageBroadcaster.broadcastToGroup(message, groupId);

        // עדכן ספירת הודעות
        if (chatService != null) {
            List<String> allMessages = chatService.getGroupMessages(groupId);
            lastSeenMessageCount = allMessages.size();
        }

        // עדכן לוקל סטורג' לכרטיסיות אחרות
        UI currentUI = UI.getCurrent();
        VaadinSession currentSession = VaadinSession.getCurrent();

        if (currentUI != null && currentUI.isAttached() && currentSession != null) {
            try {
                currentSession.lock();
                try {
                    currentUI.access(() -> {
                        currentUI.getPage().executeJs("window.notifyOtherWindows && window.notifyOtherWindows();");
                    });
                } finally {
                    currentSession.unlock();
                }
            } catch (Exception e) {
                // Log error silently
            }
        }
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
    private void replaceLastDecryptingMessage(String newMessage) {
        for (int i = chatContainer.getComponentCount() - 1; i >= 0; i--) {
            com.vaadin.flow.component.Component component = chatContainer.getComponentAt(i);
            if (component instanceof Div) {
                Div messageDiv = (Div) component;
                String innerHTML = messageDiv.getElement().getProperty("innerHTML");
                if (innerHTML != null && innerHTML.contains("[Decrypting...]")) {
                    messageDiv.getElement().setProperty("innerHTML", newMessage);

                    // עדכן סטיילינג
                    messageDiv.getStyle()
                            .set("background-color", "#e3f2fd")
                            .set("border-left", "4px solid #2196f3");

                    return;
                }
            }
        }
        // אם לא נמצאה, הוסף חדשה
        addMessageToChat(newMessage);
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
        userListContainer.removeAll();

        Group currentGroup = GroupService.getGroupById(currentGroupId);
        if (currentGroup == null || currentGroup.getUsers() == null || currentGroup.getUsers().isEmpty()) {
            userListContainer.add(new Span("No members in this group"));
            return;
        }
        // Get current user to exclude from the list
        User currentUser = UserService.getAuthenticatedUser();
        String currentUserEmail = (currentUser != null) ? currentUser.getEmail() : null;

        // Filter out the current user from the members list
        List<String> otherMembers = currentGroup.getUsers().stream()
                .filter(memberEmail -> !memberEmail.equals(currentUserEmail))
                .collect(Collectors.toList());

        // Create header and members layout
        HorizontalLayout headerAndMembersLayout = new HorizontalLayout();
        headerAndMembersLayout.setWidthFull();
        headerAndMembersLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerAndMembersLayout.setAlignItems(Alignment.CENTER);

        // Group Members header - show count of other members only
        H3 groupMembersHeader = new H3("Group Members (" + otherMembers.size() + ")");
        groupMembersHeader.getStyle().set("margin", "0");

        // Members container
        HorizontalLayout membersLayout = new HorizontalLayout();
        membersLayout.getStyle().set("flex-wrap", "wrap");
        membersLayout.setAlignItems(Alignment.CENTER);

        // Add other members (excluding current user)
        if (otherMembers.isEmpty()) {
            Span noOtherMembersSpan = new Span("Only you in this group");
            noOtherMembersSpan.getStyle()
                    .set("color", "#666")
                    .set("font-style", "italic");
            membersLayout.add(noOtherMembersSpan);
        } else {
            for (String memberEmail : otherMembers) {
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

                    // Just user icon and name (no status dot)
                    Icon userIcon = VaadinIcon.USER.create();
                    userIcon.getStyle()
                            .set("margin-right", "6px")
                            .set("color", "#666");

                    Span nameSpan = new Span(member.getFullName());
                    memberDiv.add(userIcon, nameSpan);

                    membersLayout.add(memberDiv);
                }
            }
        }
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
        User currentUser = UserService.getAuthenticatedUser();

        // Filter out the current user from the list
        List<User> availableUsers = allUsers.stream()
                .filter(user -> !user.getEmail().equals(currentUser.getEmail()))
                .collect(Collectors.toList());

        MultiSelectComboBox<User> userSelector = new MultiSelectComboBox<>("Add Members");
        userSelector.setItems(availableUsers);
        userSelector.setItemLabelGenerator(User::getFullName);
        userSelector.setWidthFull();

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button createButton = new Button("Create", e -> {
            String groupName = groupNameField.getValue().trim();
            if (groupName.isEmpty()) {
                Notification.show("Please enter a group name", 2000, Notification.Position.MIDDLE);
                return;
            }

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