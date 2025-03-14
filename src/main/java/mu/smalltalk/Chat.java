package mu.smalltalk;

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
import mu.Chatstorage;
import mu.smalltalk.Services.EncryptionService;
import mu.smalltalk.Services.GlobalMessageBroadcaster;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
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

    private final String sessionId;
    private Registration broadcasterRegistration;

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

        add(new H2(sessionId),chatContainer, messageInput, mediaUpload);

        // Load existing messages from storage
        for (String message : Chatstorage.getMessages()) {
            addMessageToChat(message);
        }

        setupMessageHandler();
        registerWithBroadcaster();
    }

    private void registerWithBroadcaster() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            // Register this UI instance with the broadcaster
            broadcasterRegistration = GlobalMessageBroadcaster.register(ui, message -> {
                ui.access(() -> {
                    addMessageToChat(message);
                });
            });
            
            // Make sure to remove the registration when the UI is detached
            ui.addDetachListener(event -> {
                if (broadcasterRegistration != null) {
                    broadcasterRegistration.remove();
                    broadcasterRegistration = null;
                }
            });
            
            // Add a connect listener to ensure the broadcaster is registered when page is loaded or refreshed
            ui.getPage().addJavaScript("window.addEventListener('load', function() { $0.onClientReconnect(); });");
            ui.getElement().executeJs("this.onClientReconnect = function() { this.$server.ensureRegistration(); }");
        }
    }
    
    // Add this method to be called when client reconnects
    public void ensureRegistration() {
        if (broadcasterRegistration == null) {
            registerWithBroadcaster();
        }
    }

    private String initializeSessionId() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session.getAttribute("sessionId") == null) {
            String uniqueId = UUID.randomUUID().toString();
            session.setAttribute("sessionId", uniqueId);
            return uniqueId;
        }
        return session.getAttribute("sessionId").toString();
    }

    private void configureMediaUpload(Upload upload, MemoryBuffer buffer) {
        Button uploadButton = new Button("Upload");
        upload.setUploadButton(uploadButton);
        upload.setAcceptedFileTypes("image/*", "audio/*");
        upload.setMaxFileSize(16 * 1024 * 1024); // 16 MB
        
        UI currentUI = UI.getCurrent();
    
        upload.addSucceededListener(event -> {
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
                    broadcastMessage("[" + timestamp + "] "  + ": ✅ Image uploaded: " + fileName + "<br>" + imageHtml);
                } else if (mimeType.startsWith("audio/")) {
                    String audioHtml = "<audio controls><source src='data:" + mimeType + ";base64," + 
                                       base64Data + "' type='" + mimeType + "'></audio>";
                    broadcastMessage("[" + timestamp + "] " +  ": ✅ Audio uploaded: " + fileName + "<br>" + audioHtml);
                }
    
                currentUI.access(() -> {
                    encryptionService.encryptAsync(fileData)
                        .thenAccept(encryptedData -> {
                            String encTimestamp = dateFormat.format(new Date());
                            currentUI.access(() -> {
                                broadcastMessage("[" + encTimestamp + "] " + ": ✅ File " + fileName + " encrypted successfully");
                            });
                        })
                        .exceptionally(ex -> {  
                            String errorTimestamp = dateFormat.format(new Date());
                            currentUI.access(() -> {
                                broadcastMessage("[" + errorTimestamp + "] " + ": ❌ Error encrypting file: " + ex.getMessage());
                            });
                            return null;
                        });
                });
            } catch (IOException e) {
                String errorTimestamp = dateFormat.format(new Date());
                broadcastMessage("[" + errorTimestamp + "] " + ": ❌ Error processing the file: " + e.getMessage());
            }
        });
    
        upload.addFailedListener(event -> {
            String errorTimestamp = dateFormat.format(new Date());
            broadcastMessage("[" + errorTimestamp + "] " + ": ❌ Upload failed: " + event.getReason());
        });
    }

    private void setupMessageHandler() {
        UI currentUI = UI.getCurrent();
        
        messageInput.addSubmitListener(submitEvent -> {
            String message = submitEvent.getValue();
            String timestamp = dateFormat.format(new Date());
            System.out.println("Received message from " + ": " + message); 
            broadcastMessage("[" + timestamp + "] " +  ": " + message); 

            currentUI.access(() -> {
                encryptionService.encryptStringAsync(message)
                    .thenAccept(encryptedMessage -> {
                        String encTimestamp = dateFormat.format(new Date());
                        currentUI.access(() -> {
                            String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessage);
                            broadcastMessage("[" + encTimestamp + "] " +  ": <b>Encrypted:</b> " + encodedMessage);
                            decryptAndDisplayMessage(encodedMessage);
                        });
                    })
                    .exceptionally(ex -> {
                        String errorTimestamp = dateFormat.format(new Date());
                        currentUI.access(() -> {
                            broadcastMessage("[" + errorTimestamp + "] " + ": <b>Error encrypting message:</b> " + ex.getMessage());
                        });
                        return null;
                    });
            });
        });
    }

    private void decryptAndDisplayMessage(String encodedMessage) {
        UI currentUI = UI.getCurrent();
        byte[] encryptedMessage = Base64.getDecoder().decode(encodedMessage);

        currentUI.access(() -> {
            encryptionService.decryptToStringAsync(encryptedMessage)
                .thenAccept(decryptedMessage -> {
                    String decTimestamp = dateFormat.format(new Date());
                    currentUI.access(() -> {
                        broadcastMessage("[" + decTimestamp + "] " + ": <b>Decrypted:</b> " + decryptedMessage);
                    });
                })
                .exceptionally(ex -> {
                    String errorTimestamp = dateFormat.format(new Date());
                    currentUI.access(() -> {
                        broadcastMessage("[" + errorTimestamp + "] " +  ": <b>Error decrypting message:</b> " + ex.getMessage());
                    });
                    return null;
                });
        });
    }
    

    private void broadcastMessage(String content) {
        // Add to storage
        Chatstorage.addMessage(content);
        
        // Broadcast to all connected clients
        GlobalMessageBroadcaster.broadcast(content);
    }

    private void addMessageToChat(String content) {
        Div messageDiv = new Div();
        messageDiv.getElement().setProperty("innerHTML", content); 
        messageDiv.getStyle().set("padding", "10px");
        messageDiv.getStyle().set("word-break", "break-word");
        messageDiv.getStyle().set("margin-bottom", "5px"); // Add space between messages
        messageDiv.getStyle().set("border-bottom", "1px solid #eee"); // Add divider between messages

        chatContainer.add(messageDiv); 
        
        // Scroll to the bottom to show the latest message
        chatContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
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