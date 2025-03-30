package mu.smalltalk;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;
import mu.Chatstorage;
import mu.smalltalk.Services.EncryptionService;
import mu.smalltalk.Services.GlobalMessageBroadcaster;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Push(PushMode.AUTOMATIC) // הוספנו Push ישירות לוויו כדי לתמוך ברענון אוטומטי
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

        add(new H2(sessionId), chatContainer, messageInput, mediaUpload);

        // טעינת הודעות קיימות מהאחסון
        for (String message : Chatstorage.getMessages()) {
            addMessageToChat(message);
        }

        setupMessageHandler();
        registerWithBroadcaster();
        
        // הוספת פונקציונליות JavaScript לזיהוי שינויים בזמן אמת
        setupClientRefreshHandling();
    }

    private void setupClientRefreshHandling() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            // הוספת קוד JavaScript שיפעיל את רענון הדף בכל פעם שמתקבלת הודעה
            ui.getPage().executeJs(
                "window.addEventListener('storage', function(e) {" +
                "   if (e.key === 'chat-update-trigger') {" +
                "       $0.onChatUpdate(e.newValue);" +
                "   }" +
                "});", getElement());
            
            // הוספת מתודה שהקוד JavaScript יכול לקרוא אליה
            getElement().executeJs(
                "this.onChatUpdate = function(timestamp) {" +
                "   this.$server.handleChatUpdate(timestamp);" +
                "}");
        }
    }
    
    // מתודה חדשה שתיקרא על ידי JavaScript כשיש עדכון
    public void handleChatUpdate(String timestamp) {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> {
                // מכיוון שהודעה חדשה נוספה כבר לשרת, אנחנו רק צריכים לרענן את ה-UI
                refreshChatContainer();
            });
        }
    }
    
    private void refreshChatContainer() {
        // מנקים את התצוגה הנוכחית
        chatContainer.removeAll();
        
        // טוענים מחדש את כל ההודעות
        for (String message : Chatstorage.getMessages()) {
            addMessageToChat(message);
        }
        
        // גלילה לתחתית הצ'אט
        chatContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    private void registerWithBroadcaster() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            // רישום ה-UI הנוכחי ל-broadcaster
            broadcasterRegistration = GlobalMessageBroadcaster.register(ui, message -> {
                ui.access(() -> {
                    addMessageToChat(message);
                    
                    // עדכון localStorage כדי לגרום לכל החלונות האחרים להתעדכן
                    ui.getPage().executeJs(
                        "localStorage.setItem('chat-update-trigger', Date.now().toString());"
                    );
                });
            });
            
            // וידוא הסרת הרישום כשהדף נסגר
            ui.addDetachListener(event -> {
                if (broadcasterRegistration != null) {
                    broadcasterRegistration.remove();
                    broadcasterRegistration = null;
                }
            });
            
            // הוספת listener לחיבור מחדש כשהדף נטען או מרוענן
            ui.getPage().executeJs(
                "window.addEventListener('load', function() { $0.onClientReconnect(); });", getElement());
            getElement().executeJs(
                "this.onClientReconnect = function() { this.$server.ensureRegistration(); }");
        }
    }
    
    // מתודה זו תיקרא כשהלקוח מתחבר מחדש
    public void ensureRegistration() {
        if (broadcasterRegistration == null) {
            registerWithBroadcaster();
        }
        
        // רענון מיידי של הצ'אט במקרה של חיבור מחדש
        refreshChatContainer();
    }

    private String initializeSessionId() {
        VaadinSession session = VaadinSession.getCurrent();
        
        // אם יש session ID ב-localStorage, נשתמש בו כדי לאפשר סשן בין דפדפנים
        UI.getCurrent().getPage().executeJs(
            "return localStorage.getItem('chat-session-id');")
            .then(String.class, result -> {
                if (result != null && !result.isEmpty()) {
                    // נשתמש ב-ID שכבר קיים
                    session.setAttribute("sessionId", result);
                    updateSessionIdDisplay(result);
                } else if (session.getAttribute("sessionId") != null) {
                    // נשמור את ה-ID הקיים ב-localStorage
                    String currentId = session.getAttribute("sessionId").toString();
                    UI.getCurrent().getPage().executeJs(
                        "localStorage.setItem('chat-session-id', $0);", currentId);
                } else {
                    // ניצור ID חדש
                    String uniqueId = UUID.randomUUID().toString();
                    session.setAttribute("sessionId", uniqueId);
                    UI.getCurrent().getPage().executeJs(
                        "localStorage.setItem('chat-session-id', $0);", uniqueId);
                }
            });
        
        // נחזיר את הערך הנוכחי או ערך זמני עד שה-JavaScript יעדכן אותו
        return session.getAttribute("sessionId") != null ? 
               session.getAttribute("sessionId").toString() : "Initializing...";
    }
    
    // מתודה לעדכון תצוגת ה-sessionId
    private void updateSessionIdDisplay(String sessionId) {
        getChildren().forEach(component -> {
            if (component instanceof H2) {
                ((H2) component).setText(sessionId);
            }
        });
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
        // הוספה לאחסון
        Chatstorage.addMessage(content);
        
        // שידור לכל הלקוחות המחוברים
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
        
        // גלילה לתחתית כדי להציג את ההודעה האחרונה
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