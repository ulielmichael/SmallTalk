package mu.smalltalk;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import mu.smalltalk.Services.EncryptionService;
import mu.smalltalk.Services.GlobalMessageBroadcaster;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Route("chat")
public class Chat extends VerticalLayout {

    private final MessageInput messageInput;
    private final Upload mediaUpload;
    private final Aes256 aes;
    private final EncryptionService encryptionService;
    private final VerticalLayout chatContainer;
    private static final int CHAT_HEIGHT = 500;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String sessionId;
    private Registration broadcasterRegistration;

    // Track the number of messages already seen - prevents duplicates
    private int lastSeenMessageCount = 0;

    public Chat() {
        sessionId = initializeSessionId();

        aes = initializeEncryption();
        encryptionService = new EncryptionService(aes);

        messageInput = new MessageInput();
        chatContainer = new VerticalLayout();

        chatContainer.setWidthFull();
        chatContainer.setHeight(CHAT_HEIGHT, Unit.PIXELS);
        chatContainer.getStyle().set("background-color", "pink");
        chatContainer.getStyle().set("overflow-y", "auto");
        chatContainer.getStyle().set("border", "1px solid black");

        messageInput.getStyle().setBackgroundColor("gray");
        messageInput.setWidthFull();

        MemoryBuffer buffer = new MemoryBuffer();
        mediaUpload = new Upload(buffer);
        configureMediaUpload(mediaUpload, buffer);

        add(new H2(sessionId), chatContainer, messageInput, mediaUpload);

        // Load existing messages initially
        loadExistingMessages();

        setupMessageHandler();

        // Setup cross-browser communication
        setupCrossBrowserCommunication();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Ensure we're registered with the broadcaster when UI is attached
        registerWithBroadcaster();

        // Check for any new messages immediately
        checkForNewMessages();

        // Set up polling as a fallback mechanism
        UI ui = attachEvent.getUI();
        if (ui != null) {
            ui.setPollInterval(1000); // 1-second polling as backup
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        // Clean up broadcaster registration
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    private void loadExistingMessages() {
        List<String> existingMessages = Chatstorage.getMessages();
        for (String message : existingMessages) {
            addMessageToChat(message);
        }
        lastSeenMessageCount = existingMessages.size();

        // Scroll to bottom after loading messages
        scrollToBottom();
    }

    private void setupMessageHandler() {
        // Store UI and session reference before async operations
        UI currentUI = UI.getCurrent();
        VaadinSession currentSession = VaadinSession.getCurrent();

        messageInput.addSubmitListener(submitEvent -> {
            String message = submitEvent.getValue();
            String timestamp = dateFormat.format(new Date());
            System.out.println("Received message from " + sessionId + ": " + message);

            // Send original message to all users
            sendMessage("[" + timestamp + "] " + sessionId + ": " + message);

            // Encrypt and decrypt message only for sending user (local only)
            encryptionService.encryptStringAsync(message)
                    .thenAccept(encryptedMessage -> {
                        // Check if UI is still valid and attached
                        if (currentUI.isAttached() && !currentSession.getSession().isNew()) {
                            currentSession.lock();
                            try {
                                currentUI.access(() -> {
                                    String encTimestamp = dateFormat.format(new Date());
                                    String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessage);

                                    // Show encrypted message locally only
                                    addLocalMessage("[" + encTimestamp + "] " + sessionId + ": <b>Encrypted:</b> "
                                            + encodedMessage);

                                    // Decrypt and display locally only
                                    decryptAndDisplayMessage(encodedMessage, true);
                                    currentUI.push();
                                });
                            } finally {
                                currentSession.unlock();
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        // Check if UI is still valid and attached
                        if (currentUI.isAttached() && !currentSession.getSession().isNew()) {
                            currentSession.lock();
                            try {
                                currentUI.access(() -> {
                                    String errorTimestamp = dateFormat.format(new Date());
                                    addLocalMessage("[" + errorTimestamp + "] " + sessionId
                                            + ": <b>Error encrypting message:</b> " + ex.getMessage());
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
            // Listen for localStorage events (triggered when localStorage changes in other
            // tabs/windows)
            ui.getPage().executeJs(
                    "window.addEventListener('storage', function(e) {" +
                            "   if (e.key === 'chat-update-trigger') {" +
                            "       $0.$server.handleChatUpdate(e.newValue);" +
                            "   }" +
                            "});",
                    getElement());
        }
    }

    // Called by JavaScript when localStorage changes (for same browser, different
    // tabs)
    public void handleChatUpdate(String timestamp) {
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                checkForNewMessages();
                ui.push();
            });
        }
    }

    // Check for new messages periodically
    public void checkForNewMessages() {
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                List<String> allMessages = Chatstorage.getMessages();
                int currentMessageCount = allMessages.size();

                if (currentMessageCount > lastSeenMessageCount) {
                    // Add only new messages
                    for (int i = lastSeenMessageCount; i < currentMessageCount; i++) {
                        addMessageToChat(allMessages.get(i));
                    }
                    lastSeenMessageCount = currentMessageCount;

                    // Scroll to bottom
                    scrollToBottom();
                    ui.push();
                }
            });
        }
    }

    private void registerWithBroadcaster() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            // Remove any existing registration
            if (broadcasterRegistration != null) {
                broadcasterRegistration.remove();
                broadcasterRegistration = null;
            }

            System.out.println("Registering UI: " + ui.getUIId() + " with broadcaster");

            // Register this UI with the broadcaster
            broadcasterRegistration = GlobalMessageBroadcaster.register(ui, message -> {
                if (ui.isAttached()) {
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

    // Called when client reconnects
    public void ensureRegistration() {
        if (broadcasterRegistration == null || !GlobalMessageBroadcaster.isRegistered(UI.getCurrent())) {
            System.out.println("Re-registering with broadcaster after reconnection");
            registerWithBroadcaster();
        }

        // Refresh chat immediately after reconnection
        checkForNewMessages();
    }

    private String initializeSessionId() {
        VaadinSession session = VaadinSession.getCurrent();
        String uniqueId;

        // Create new unique ID if needed
        if (session.getAttribute("sessionId") == null) {
            uniqueId = "User-" + UUID.randomUUID().toString().substring(0, 8);
            session.setAttribute("sessionId", uniqueId);

            // Store in localStorage for persistence across browser sessions
            UI.getCurrent().getPage().executeJs(
                    "localStorage.setItem('chat-session-id', $0);", uniqueId);
        } else {
            uniqueId = session.getAttribute("sessionId").toString();
        }

        // Check localStorage for existing ID (helps with browser refresh)
        UI.getCurrent().getPage().executeJs(
                "return localStorage.getItem('chat-session-id');")
                .then(String.class, result -> {
                    if (result != null && !result.isEmpty()) {
                        // Update session with stored ID if it exists
                        session.setAttribute("sessionId", result);
                        updateSessionIdDisplay(result);
                        this.sessionId = result;
                    }
                });

        return uniqueId;
    }

    private void updateSessionIdDisplay(String id) {
        getChildren().forEach(component -> {
            if (component instanceof H2) {
                ((H2) component).setText(id);
            }
        });
    }

    private void configureMediaUpload(Upload upload, MemoryBuffer buffer) {
        Button uploadButton = new Button("Upload");
        upload.setUploadButton(uploadButton);
        upload.setAcceptedFileTypes("image/*", "audio/*");
        upload.setMaxFileSize(16 * 1024 * 1024); // 16 MB

        upload.addSucceededListener(event -> {
            // Store UI and session reference before async operations
            UI ui = UI.getCurrent();
            VaadinSession session = VaadinSession.getCurrent();
            
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
                    sendMessage("[" + timestamp + "] " + sessionId + ": ✅ Image uploaded: " + fileName + "<br>"
                            + imageHtml);
                } else if (mimeType.startsWith("audio/")) {
                    String audioHtml = "<audio controls><source src='data:" + mimeType + ";base64," +
                            base64Data + "' type='" + mimeType + "'></audio>";
                    sendMessage("[" + timestamp + "] " + sessionId + ": ✅ Audio uploaded: " + fileName + "<br>"
                            + audioHtml);
                }

                // Process encryption in background thread with proper session handling
                final byte[] finalFileData = fileData;
                encryptionService.encryptAsync(finalFileData)
                    .thenAccept(encryptedData -> {
                        // Check if UI is still valid and attached
                        if (ui.isAttached() && !session.getSession().isNew()) {
                            session.lock();
                            try {
                                ui.access(() -> {
                                    String encTimestamp = dateFormat.format(new Date());
                                    addLocalMessage("[" + encTimestamp + "] " + sessionId + ": ✅ File " + fileName
                                            + " encrypted successfully");
                                    ui.push();
                                });
                            } finally {
                                session.unlock();
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        // Check if UI is still valid and attached
                        if (ui.isAttached() && !session.getSession().isNew()) {
                            session.lock();
                            try {
                                ui.access(() -> {
                                    String errorTimestamp = dateFormat.format(new Date());
                                    addLocalMessage("[" + errorTimestamp + "] " + sessionId
                                            + ": ❌ Error encrypting file: " + ex.getMessage());
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
                        "[" + errorTimestamp + "] " + sessionId + ": ❌ Error processing the file: " + e.getMessage());
            }
        });

        upload.addFailedListener(event -> {
            String errorTimestamp = dateFormat.format(new Date());
            sendMessage("[" + errorTimestamp + "] " + sessionId + ": ❌ Upload failed: " + event.getReason());
        });
    }

    // Method to add local-only messages (no broadcasting)
    private void addLocalMessage(String content) {
        addMessageToChat(content);
    }

    // Method to send message to all users
    private void sendMessage(String content) {
        // Broadcast to all connected clients (prevents duplicates)
        GlobalMessageBroadcaster.broadcast(content);
    }

    private void scrollToBottom() {
        // Scroll chat container to bottom to show latest messages
        chatContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    private void decryptAndDisplayMessage(String encodedMessage, boolean localOnly) {
        // Store UI and session reference before async operations
        UI ui = UI.getCurrent();
        VaadinSession session = VaadinSession.getCurrent();
        
        byte[] encryptedMessage = Base64.getDecoder().decode(encodedMessage);

        encryptionService.decryptToStringAsync(encryptedMessage)
            .thenAccept(decryptedMessage -> {
                // Check if UI is still valid and attached
                if (ui.isAttached() && !session.getSession().isNew()) {
                    session.lock();
                    try {
                        ui.access(() -> {
                            String decTimestamp = dateFormat.format(new Date());
                            String displayMessage = "[" + decTimestamp + "] " + sessionId + ": <b>Decrypted:</b> "
                                    + decryptedMessage;

                            if (localOnly) {
                                // Display to this user only
                                addLocalMessage(displayMessage);
                            } else {
                                // Broadcast to all users
                                sendMessage(displayMessage);
                            }

                            ui.push();
                        });
                    } finally {
                        session.unlock();
                    }
                }
            })
            .exceptionally(ex -> {
                // Check if UI is still valid and attached
                if (ui.isAttached() && !session.getSession().isNew()) {
                    session.lock();
                    try {
                        ui.access(() -> {
                            String errorTimestamp = dateFormat.format(new Date());
                            String errorMessage = "[" + errorTimestamp + "] " + sessionId
                                    + ": <b>Error decrypting message:</b> " + ex.getMessage();

                            if (localOnly) {
                                addLocalMessage(errorMessage);
                            } else {
                                sendMessage(errorMessage);
                            }

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
        messageDiv.getStyle().set("border-bottom", "1px solid #eee");

        chatContainer.add(messageDiv);

        // Scroll to bottom to show latest message
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