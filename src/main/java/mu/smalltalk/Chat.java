package mu.smalltalk;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;

import mu.smalltalk.Services.EncryptionService;

import com.flowingcode.vaadin.addons.chatassistant.ChatAssistant;
import com.flowingcode.vaadin.addons.chatassistant.model.Message;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Base64;

@Route("chat")
public class Chat extends VerticalLayout {

    private final MessageInput messageInput;
    private final Upload mediaUpload;
    private final Aes256 aes;
    private final EncryptionService encryptionService;

    private final ChatAssistant chatAssistant;

    private static final String username = "Michaelangelos";
    private static final int CHAT_HEIGHT = 500;

    public Chat() {
        aes = initializeEncryption();
        encryptionService = new EncryptionService(aes);

        messageInput = new MessageInput();

        chatAssistant = new ChatAssistant(true);

        chatAssistant.setWidthFull();
        chatAssistant.setHeight(CHAT_HEIGHT, Unit.PIXELS);

        messageInput.getStyle().setBackgroundColor("cyan");
        chatAssistant.getStyle().set("background-color", "lightyellow");

        MemoryBuffer buffer = new MemoryBuffer();
        mediaUpload = new Upload(buffer);
        configureMediaUpload(mediaUpload, buffer);

        messageInput.setWidthFull();

        add(chatAssistant, messageInput, mediaUpload);

        setupMessageHandler();
    }

    private void configureMediaUpload(Upload upload, MemoryBuffer buffer) {
        Button uploadButton = new Button("Upload");
        upload.setUploadButton(uploadButton);
        upload.setAcceptedFileTypes("image/*", "audio/*");
        upload.setMaxFileSize(16 * 1024 * 1024);

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            String mimeType = event.getMIMEType();

            try {
                InputStream inputStream = buffer.getInputStream();
                byte[] fileData = inputStream.readAllBytes();
                String encodedString = Base64.getEncoder().encodeToString(fileData);

                if (mimeType.startsWith("image/")) {
                    String markdownImage = "![Image](data:image/png;base64," + encodedString + ")";
                    sendMarkdownMessage("✅ The file " + fileName + "\n" + markdownImage);
                } else if (mimeType.startsWith("audio/")) {
                    String audioPlayerHTML = "<audio controls><source src='data:audio/mpeg;base64," + encodedString
                            + "' type='" + mimeType + "'></audio>";
                    sendMarkdownMessage("✅ The file " + fileName + "\n" + audioPlayerHTML);
                }

            } catch (IOException e) {
                sendMarkdownMessage("❌ Error processing the file: " + e.getMessage());
            }
        });

        upload.addFailedListener(event -> sendMarkdownMessage("❌ Upload failed: " + event.getReason()));
    }

    private void setupMessageHandler() {
        messageInput.addSubmitListener(submitEvent -> {
            String message = submitEvent.getValue();

            sendMarkdownMessage(message);
        });
    }

    /**
     * Send a message with Markdown format to the chat
     *
     * @param markdownContent Message content in Markdown format
     */
    private void sendMarkdownMessage(String markdownContent) {
        Message message = Message.builder()
                .content(markdownContent)
                .messageTime(LocalDateTime.now())
                .name(username)
                .avatar("user-avatar.png")
                .build();

        chatAssistant.sendMessage(message);
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
