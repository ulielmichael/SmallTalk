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
    
    // מונה את מספר ההודעות שכבר נצפו - משמש למניעת כפילויות
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

        // טעינה התחלתית של הודעות קיימות
        List<String> existingMessages = Chatstorage.getMessages();
        for (String message : existingMessages) {
            addMessageToChat(message);
        }
        lastSeenMessageCount = existingMessages.size();

        setupMessageHandler();
        registerWithBroadcaster();
        
        // הגדרת תקשורת בין דפדפנים
        setupCrossBrowserCommunication();
    }

    private void setupCrossBrowserCommunication() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            // מאזין JavaScript לאירועי localStorage (מופעל כאשר localStorage משתנה בטאבים/חלונות אחרים)
            ui.getPage().executeJs(
                "window.addEventListener('storage', function(e) {" +
                "   if (e.key === 'chat-update-trigger') {" +
                "       $0.$server.handleChatUpdate(e.newValue);" +
                "   }" +
                "});", getElement());
            
            // בדיקת הודעות חדשות כל שנייה כפתרון חלופי לסנכרון בין דפדפנים
            ui.getPage().executeJs(
                "setInterval(function() {" +
                "   $0.$server.checkForNewMessages();" +
                "}, 1000);", getElement());
        }
    }
    
    // נקרא על ידי JavaScript כאשר localStorage משתנה (עבור אותו דפדפן, טאבים שונים)
    public void handleChatUpdate(String timestamp) {
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                checkForNewMessages(); // בדיקה רק להודעות חדשות במקום טעינה מחדש של כל ההודעות
                ui.push();
            });
        }
    }
    
    // בדיקה תקופתית להודעות חדשות (עובדת בין דפדפנים שונים)
    public void checkForNewMessages() {
        UI ui = UI.getCurrent();
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                List<String> allMessages = Chatstorage.getMessages();
                int currentMessageCount = allMessages.size();
                
                if (currentMessageCount > lastSeenMessageCount) {
                    // הוספת הודעות חדשות בלבד
                    for (int i = lastSeenMessageCount; i < currentMessageCount; i++) {
                        addMessageToChat(allMessages.get(i));
                    }
                    lastSeenMessageCount = currentMessageCount;
                    
                    // גלילה לתחתית
                    chatContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
                    ui.push();
                }
            });
        }
    }
    
    // שינוי מתודת הרענון להוספת הודעות חדשות בלבד במקום טעינה מחדש של כל התוכן
    public void refreshChatContainer() {
        checkForNewMessages();
    }

    private void registerWithBroadcaster() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            System.out.println("Registering UI: " + ui.getUIId() + " with broadcaster");
            
            // רישום ה-UI הנוכחי ב-broadcaster
            broadcasterRegistration = GlobalMessageBroadcaster.register(ui, message -> {
                if (ui.isAttached()) {
                    ui.access(() -> {
                        addMessageToChat(message);
                        ui.push();
                    });
                }
            });
            
            if (broadcasterRegistration == null) {
                System.err.println("Failed to register with broadcaster for UI: " + ui.getUIId());
                Notification.show("Failed to connect to the chat server. Please refresh the page.");
            }
            
            // וידוא שהרישום מוסר כאשר הדף נסגר
            ui.addDetachListener(event -> {
                System.out.println("UI Detached: " + ui.getUIId());
                if (broadcasterRegistration != null) {
                    broadcasterRegistration.remove();
                    broadcasterRegistration = null;
                }
            });
            
            // הוספת מאזין להתחברות מחדש
            ui.getPage().executeJs(
                "window.addEventListener('load', function() { $0.$server.ensureRegistration(); });", getElement());
        } else {
            System.err.println("Cannot register with broadcaster - UI is null");
        }
    }
    
    // נקרא כאשר הלקוח מתחבר מחדש
    public void ensureRegistration() {
        if (broadcasterRegistration == null) {
            System.out.println("Re-registering with broadcaster after reconnection");
            registerWithBroadcaster();
        }
        
        // רענון מיידי של הצ'אט בעת התחברות מחדש
        checkForNewMessages();
    }

    private String initializeSessionId() {
        VaadinSession session = VaadinSession.getCurrent();
        String uniqueId;
        
        // יצירת מזהה ייחודי חדש אם צריך
        if (session.getAttribute("sessionId") == null) {
            uniqueId = "User-" + UUID.randomUUID().toString().substring(0, 8);
            session.setAttribute("sessionId", uniqueId);
            
            // שמירה ב-localStorage לשימור בין הפעלות דפדפן
            UI.getCurrent().getPage().executeJs(
                "localStorage.setItem('chat-session-id', $0);", uniqueId);
        } else {
            uniqueId = session.getAttribute("sessionId").toString();
        }
        
        // בדיקת localStorage למזהה קיים (עוזר ברענון דפדפן)
        UI.getCurrent().getPage().executeJs(
            "return localStorage.getItem('chat-session-id');")
            .then(String.class, result -> {
                if (result != null && !result.isEmpty()) {
                    // עדכון המפגש עם המזהה המאוחסן אם הוא קיים
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
                    sendMessage("[" + timestamp + "] " + sessionId + ": ✅ Image uploaded: " + fileName + "<br>" + imageHtml);
                } else if (mimeType.startsWith("audio/")) {
                    String audioHtml = "<audio controls><source src='data:" + mimeType + ";base64," + 
                                       base64Data + "' type='" + mimeType + "'></audio>";
                    sendMessage("[" + timestamp + "] " + sessionId + ": ✅ Audio uploaded: " + fileName + "<br>" + audioHtml);
                }
    
                // מציג את הודעת ההצפנה רק למשתמש המקומי
                currentUI.access(() -> {
                    encryptionService.encryptAsync(fileData)
                        .thenAccept(encryptedData -> {
                            String encTimestamp = dateFormat.format(new Date());
                            currentUI.access(() -> {
                                // הצג הודעת הצפנה מקומית בלבד
                                addLocalMessage("[" + encTimestamp + "] " + sessionId + ": ✅ File " + fileName + " encrypted successfully");
                                currentUI.push();
                            });
                        })
                        .exceptionally(ex -> {  
                            String errorTimestamp = dateFormat.format(new Date());
                            currentUI.access(() -> {
                                addLocalMessage("[" + errorTimestamp + "] " + sessionId + ": ❌ Error encrypting file: " + ex.getMessage());
                                currentUI.push();
                            });
                            return null;
                        });
                });
            } catch (IOException e) {
                String errorTimestamp = dateFormat.format(new Date());
                sendMessage("[" + errorTimestamp + "] " + sessionId + ": ❌ Error processing the file: " + e.getMessage());
            }
        });
    
        upload.addFailedListener(event -> {
            String errorTimestamp = dateFormat.format(new Date());
            sendMessage("[" + errorTimestamp + "] " + sessionId + ": ❌ Upload failed: " + event.getReason());
        });
    }

    // מתודה חדשה להוספת הודעות מקומיות בלבד (ללא שידור)
    private void addLocalMessage(String content) {
        addMessageToChat(content);
    }

    // מתודה חדשה לשליחת הודעה לכל המשתמשים
    private void sendMessage(String content) {
        // שידור לכל הלקוחות המחוברים בלבד (מניעת כפילויות)
        GlobalMessageBroadcaster.broadcast(content);
    }

    private void setupMessageHandler() {
        UI currentUI = UI.getCurrent();
        
        messageInput.addSubmitListener(submitEvent -> {
            String message = submitEvent.getValue();
            String timestamp = dateFormat.format(new Date());
            System.out.println("Received message from " + sessionId + ": " + message); 
            
            // שליחת ההודעה המקורית בלבד לכל המשתמשים
            sendMessage("[" + timestamp + "] " + sessionId + ": " + message); 

            // הצפנה ופענוח הודעה רק למשתמש השולח (מקומי בלבד)
            currentUI.access(() -> {
                encryptionService.encryptStringAsync(message)
                    .thenAccept(encryptedMessage -> {
                        String encTimestamp = dateFormat.format(new Date());
                        currentUI.access(() -> {
                            String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessage);
                            
                            // הצגת הודעה מוצפנת מקומית בלבד
                            addLocalMessage("[" + encTimestamp + "] " + sessionId + ": <b>Encrypted:</b> " + encodedMessage);
                            
                            // פענוח והצגה מקומית בלבד
                            decryptAndDisplayMessage(encodedMessage, true);
                            currentUI.push();
                        });
                    })
                    .exceptionally(ex -> {
                        String errorTimestamp = dateFormat.format(new Date());
                        currentUI.access(() -> {
                            addLocalMessage("[" + errorTimestamp + "] " + sessionId + ": <b>Error encrypting message:</b> " + ex.getMessage());
                            currentUI.push();
                        });
                        return null;
                    });
            });
        });
    }

    // מתודה מעודכנת לפענוח והצגת הודעה עם דגל שמציין אם זו הודעה מקומית בלבד
    private void decryptAndDisplayMessage(String encodedMessage, boolean localOnly) {
        UI currentUI = UI.getCurrent();
        byte[] encryptedMessage = Base64.getDecoder().decode(encodedMessage);

        currentUI.access(() -> {
            encryptionService.decryptToStringAsync(encryptedMessage)
                .thenAccept(decryptedMessage -> {
                    String decTimestamp = dateFormat.format(new Date());
                    currentUI.access(() -> {
                        String displayMessage = "[" + decTimestamp + "] " + sessionId + ": <b>Decrypted:</b> " + decryptedMessage;
                        
                        if (localOnly) {
                            // הצגה למשתמש זה בלבד
                            addLocalMessage(displayMessage);
                        } else {
                            // שידור לכל המשתמשים
                            sendMessage(displayMessage);
                        }
                        
                        currentUI.push();
                    });
                })
                .exceptionally(ex -> {
                    String errorTimestamp = dateFormat.format(new Date());
                    currentUI.access(() -> {
                        String errorMessage = "[" + errorTimestamp + "] " + sessionId + ": <b>Error decrypting message:</b> " + ex.getMessage();
                        
                        if (localOnly) {
                            addLocalMessage(errorMessage);
                        } else {
                            sendMessage(errorMessage);
                        }
                        
                        currentUI.push();
                    });
                    return null;
                });
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
        
        // גלילה לתחתית להצגת ההודעה האחרונה
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