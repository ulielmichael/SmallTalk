package mu.smalltalk;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
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
import java.util.Base64;
import java.util.UUID;

@Route("chat")
public class Chat extends VerticalLayout {

    private final MessageInput messageInput;
    private final Upload mediaUpload;
    private final Aes256 aes;
    private final EncryptionService encryptionService;
    private final VerticalLayout chatContainer; 
    private static final int CHAT_HEIGHT = 500;

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
        chatContainer.getStyle().set("background-color", "lightyellow");
        chatContainer.getStyle().set("overflow-y", "auto");
        chatContainer.getStyle().set("border", "1px solid black");

        messageInput.getStyle().setBackgroundColor("cyan");
        messageInput.setWidthFull();

        MemoryBuffer buffer = new MemoryBuffer();
        mediaUpload = new Upload(buffer);
        configureMediaUpload(mediaUpload, buffer);

        add(chatContainer, messageInput, mediaUpload);

        for (String message : Chatstorage.getMessages()) {
            addMessageToChat(message);
        }

        setupMessageHandler();
        registerWithBroadcaster();
    }

    private void registerWithBroadcaster() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            broadcasterRegistration = GlobalMessageBroadcaster.register(ui, message -> {
                ui.access(() -> {
                    addMessageToChat(message);
                });
            });
            
            ui.addDetachListener(event -> {
                if (broadcasterRegistration != null) {
                    broadcasterRegistration.remove();
                    broadcasterRegistration = null;
                }
            });
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
    
                if (mimeType.startsWith("image/")) {
                    String imageHtml = "<img src='data:" + mimeType + ";base64," + base64Data + 
                                       "' alt='Image' style='max-width: 100%; max-height: 300px;'>";
                    broadcastMessage(sessionId + ": ✅ Image uploaded: " + fileName + "<br>" + imageHtml);
                } else if (mimeType.startsWith("audio/")) {
                    String audioHtml = "<audio controls><source src='data:" + mimeType + ";base64," + 
                                       base64Data + "' type='" + mimeType + "'></audio>";
                    broadcastMessage(sessionId + ": ✅ Audio uploaded: " + fileName + "<br>" + audioHtml);
                }
    
                currentUI.access(() -> {
                    encryptionService.encryptAsync(fileData)
                        .thenAccept(encryptedData -> {
                            currentUI.access(() -> {
                                broadcastMessage(sessionId + ": ✅ File " + fileName + " encrypted successfully");
                            });
                        })
                        .exceptionally(ex -> {  
                            currentUI.access(() -> {
                                broadcastMessage(sessionId + ": ❌ Error encrypting file: " + ex.getMessage());
                            });
                            return null;
                        });
                });
            } catch (IOException e) {
                broadcastMessage(sessionId + ": ❌ Error processing the file: " + e.getMessage());
            }
        });
    
        upload.addFailedListener(event -> broadcastMessage(sessionId + ": ❌ Upload failed: " + event.getReason()));
    }

    private void setupMessageHandler() {
        UI currentUI = UI.getCurrent();
        
        messageInput.addSubmitListener(submitEvent -> {
            String message = submitEvent.getValue();
            System.out.println("Received message from " + sessionId + ": " + message); 
            broadcastMessage(sessionId + ": You: " + message); 

            currentUI.access(() -> {
                encryptionService.encryptStringAsync(message)
                    .thenAccept(encryptedMessage -> {
                        currentUI.access(() -> {
                            String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessage);
                            broadcastMessage(sessionId + ": <b>Encrypted:</b> " + encodedMessage);
                            decryptAndDisplayMessage(encodedMessage);
                        });
                    })
                    .exceptionally(ex -> {
                        currentUI.access(() -> {
                            broadcastMessage(sessionId + ": <b>Error encrypting message:</b> " + ex.getMessage());
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
                    currentUI.access(() -> {
                        broadcastMessage(sessionId + ": <b>Decrypted:</b> " + decryptedMessage);
                    });
                })
                .exceptionally(ex -> {
                    currentUI.access(() -> {
                        broadcastMessage(sessionId + ": <b>Error decrypting message:</b> " + ex.getMessage());
                    });
                    return null;
                });
        });
    }

    private void broadcastMessage(String content) {
        Chatstorage.addMessage(content);
        
        GlobalMessageBroadcaster.broadcast(content);
        
   
    }

    private void addMessageToChat(String content) {
        Div messageDiv = new Div();
        messageDiv.getElement().setProperty("innerHTML", content); 
        messageDiv.getStyle().set("padding", "10px");
        messageDiv.getStyle().set("word-break", "break-word");

        chatContainer.add(messageDiv); 
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