package mu.smalltalk;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

import mu.smalltalk.Services.EncryptionService;
import mu.smalltalk.Services.GlobalMessageBroadcaster;
import mu.smalltalk.Services.UserService;
import mu.smalltalk.entitis.User;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Route("chat")
public class Chat extends VerticalLayout implements BeforeEnterObserver {

    private final MessageInput messageInput;
    private final Upload mediaUpload;
    private final Aes256 aes;
    private final EncryptionService encryptionService;
    private final VerticalLayout chatContainer;
    private static final int CHAT_HEIGHT = 500;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String sessionId;
    private Registration broadcasterRegistration;

    private int lastSeenMessageCount = 0;
    
    // Add a refresh button for the chat
    private Button refreshButton;
    
    // Add theme toggle button
    private Button themeToggleButton;
    
    // Track current theme
    private boolean isDarkMode = false;

    public Chat() {
        sessionId = initializeSessionId();

        aes = initializeEncryption();
        encryptionService = new EncryptionService(aes);

        messageInput = new MessageInput();
        chatContainer = new VerticalLayout();

        chatContainer.setWidthFull();
        chatContainer.setHeight(CHAT_HEIGHT, Unit.PIXELS);
        
        // Default to light mode
        applyLightMode();
        
        chatContainer.getStyle().set("overflow-y", "auto");
        chatContainer.getStyle().set("border", "1px solid #ddd");

        messageInput.setWidthFull();

        MemoryBuffer buffer = new MemoryBuffer();
        mediaUpload = new Upload(buffer);
        configureMediaUpload(mediaUpload, buffer);
        
        // Create refresh button
        refreshButton = new Button("Refresh Chat");
        refreshButton.addClickListener(e -> refreshChatHistory());
        
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
        
        // Create action buttons layout
        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.add(refreshButton);
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

        add(toolbar, header, chatContainer, messageInput, mediaUpload, actionButtons);

        loadExistingMessages();

        setupMessageHandler();

        setupCrossBrowserCommunication();
        
        loadThemePreference();
    }
    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!UserService.isUserAuthenticated()) {
            event.forwardTo("login");  
            
            Notification.show("Please log in to access the chat", 
                           3000, Notification.Position.MIDDLE);
        }
    }

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
                
        refreshChatHistory();
    }
    
    private void applyDarkMode() {
        getStyle().set("background-color", "#2c2c2c");
        getStyle().set("color", "#f0f0f0");
        
        chatContainer.getStyle().set("background-color", "#1e1e1e");
        chatContainer.getStyle().set("border", "1px solid #444");
        
        messageInput.getStyle().set("background-color", "#333");
        messageInput.getStyle().set("color", "white");
    }
    
    private void applyLightMode() {
        getStyle().set("background-color", "#f8f8f8");
        getStyle().set("color", "#333");
        
        chatContainer.getStyle().set("background-color", "white");
        chatContainer.getStyle().set("border", "1px solid #ddd");
        
        messageInput.getStyle().set("background-color", "white");
        messageInput.getStyle().set("color", "#333");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registerWithBroadcaster();

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
    }

    private void loadExistingMessages() {
        List<String> existingMessages = Chatstorage.getMessages();
        
        chatContainer.removeAll();
        
        for (String message : existingMessages) {
            addMessageToChat(message);
        }
        lastSeenMessageCount = existingMessages.size();

        scrollToBottom();
    }
    
    private void refreshChatHistory() {
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                loadExistingMessages();
                ui.push();
                Notification.show("Chat refreshed", 2000, Notification.Position.BOTTOM_CENTER);
            });
        }
    }

    private void setupMessageHandler() {
        UI currentUI = UI.getCurrent();
        VaadinSession currentSession = VaadinSession.getCurrent();

        messageInput.addSubmitListener(submitEvent -> {
            String message = submitEvent.getValue();
            String timestamp = dateFormat.format(new Date());
            
            User currentUser = UserService.getAuthenticatedUser();
            String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;
            
            System.out.println("Received message from " + displayName + ": " + message);

            String formattedMessage = "[" + timestamp + "] " + displayName + ": " + message;
            sendMessage(formattedMessage);
            
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
                                    
                                    sendMessage(encryptedFormattedMessage);

                                    decryptAndDisplayMessage(encodedMessage, false);
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
                                    
                                    sendMessage(errorMessage);
                                    
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
                ui.push();
            });
        }
    }

    public void checkForNewMessages() {
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                List<String> allMessages = Chatstorage.getMessages();
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

            broadcasterRegistration = GlobalMessageBroadcaster.register(ui, message -> {
                if (ui.isAttached()) {
                    ui.access(() -> {
                        addMessageToChat(message);
                        
                        lastSeenMessageCount = Chatstorage.getMessages().size();
                        
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
                        ((H2) child).setText(id);
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
                    sendMessage("[" + timestamp + "] " + displayName + ": ✅ Image uploaded: " + fileName + "<br>"
                            + imageHtml);
                } else if (mimeType.startsWith("audio/")) {
                    String audioHtml = "<audio controls><source src='data:" + mimeType + ";base64," +
                            base64Data + "' type='" + mimeType + "'></audio>";
                    sendMessage("[" + timestamp + "] " + displayName + ": ✅ Audio uploaded: " + fileName + "<br>"
                            + audioHtml);
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
                                    
                                    sendMessage(successMessage);
                                    
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
                                    
                                    sendMessage(errorMessage);
                                    
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
                sendMessage(
                        "[" + errorTimestamp + "] " + displayName + ": ❌ Error processing the file: " + e.getMessage());
            }
        });

        upload.addFailedListener(event -> {
            User currentUser = UserService.getAuthenticatedUser();
            String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;
            
            String errorTimestamp = dateFormat.format(new Date());
            sendMessage("[" + errorTimestamp + "] " + displayName + ": ❌ Upload failed: " + event.getReason());
        });
    }

    private void sendMessage(String content) {
        GlobalMessageBroadcaster.broadcast(content);
        
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                checkForNewMessages();
                ui.push();
            });
        }
    }

    private void scrollToBottom() {
        chatContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    private void decryptAndDisplayMessage(String encodedMessage, boolean localOnly) {
        UI ui = UI.getCurrent();
        VaadinSession session = VaadinSession.getCurrent();
        
        User currentUser = UserService.getAuthenticatedUser();
        String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;
        
        byte[] encryptedMessage = Base64.getDecoder().decode(encodedMessage);

        encryptionService.decryptToStringAsync(encryptedMessage)
            .thenAccept(decryptedMessage -> {
                if (ui.isAttached() && !session.getSession().isNew()) {
                    session.lock();
                    try {
                        ui.access(() -> {
                            String decTimestamp = dateFormat.format(new Date());
                            String displayMessage = "[" + decTimestamp + "] " + displayName + ": <b>Decrypted:</b> "
                                    + decryptedMessage;

                            sendMessage(displayMessage);

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
                                    + ": <b>Error decrypting message:</b> " + ex.getMessage();

                            sendMessage(errorMessage);

                            ui.push();
                        });
                    } finally {
                        session.unlock();
                    }
                }
                return null;
            });
    }

    private void addMessageToChat(String content) {
        Div messageDiv = new Div();
        messageDiv.getElement().setProperty("innerHTML", content);
        messageDiv.getStyle().set("padding", "10px");
        messageDiv.getStyle().set("word-break", "break-word");
        messageDiv.getStyle().set("margin-bottom", "5px");
        messageDiv.getStyle().set("border-bottom", "1px solid " + (isDarkMode ? "#444" : "#eee"));
        
        User currentUser = UserService.getAuthenticatedUser();
        String displayName = (currentUser != null) ? currentUser.getFullName() : sessionId;
        
        if (content.contains(displayName)) {
            if (isDarkMode) {
                messageDiv.getStyle().set("background-color", "#2d3748");
                messageDiv.getStyle().set("border-left", "3px solid #63b3ed");
            } else {
                messageDiv.getStyle().set("background-color", "#ebf8ff");
                messageDiv.getStyle().set("border-left", "3px solid #3182ce");
            }
        } else {
            if (isDarkMode) {
                messageDiv.getStyle().set("background-color", "#2a2f3a");
                messageDiv.getStyle().set("border-left", "3px solid #718096");
            } else {
                messageDiv.getStyle().set("background-color", "#f7fafc");
                messageDiv.getStyle().set("border-left", "3px solid #a0aec0");
            }
        }

        chatContainer.add(messageDiv);
        scrollToBottom();
    }

    private Aes256 initializeEncryption() {
        try {
            byte[] key = new byte[32];
            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) i;
            }
            return new Aes256(key);
        } catch (Exception e) {
            Notification.show("Error initializing encryption");
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }
}