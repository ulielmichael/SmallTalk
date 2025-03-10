package mu.smalltalk;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
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
    private final Div mediaContainer;

    private final ChatAssistant chatAssistant;

    private static final String username = "Michaelangelos";
    private static final int CHAT_HEIGHT = 500;

    public Chat() {
        aes = initializeEncryption();
        encryptionService = new EncryptionService(aes);

        messageInput = new MessageInput();
        mediaContainer = new Div();

        chatAssistant = new ChatAssistant(true);

        chatAssistant.setWidthFull();
        chatAssistant.setHeight(CHAT_HEIGHT, Unit.PIXELS);

        messageInput.getStyle().setBackgroundColor("cyan");
        chatAssistant.getStyle().set("background-color", "lightgray");

        mediaContainer.getStyle().set("width", "100%");
        mediaContainer.getStyle().set("padding", "10px");
        mediaContainer.getStyle().set("background-color", "lightgreen");
        mediaContainer.getStyle().set("margin-top", "10px");

        MemoryBuffer buffer = new MemoryBuffer();
        mediaUpload = new Upload(buffer);
        configureMediaUpload(mediaUpload, buffer);

        messageInput.setWidthFull();

        add(chatAssistant, messageInput, mediaUpload, mediaContainer);

        setupMessageHandler();

        // sendMarkdownMessage(
        //         "# Welcome to the Chat!\n\nThis chat supports **Markdown** and you can use it in the following ways:\n\n"
        //                 +
        //                 "- *Italic text* (`*Italic text*`)\n" +
        //                 "- **Bold text** (`**Bold text**`)\n" +
        //                 "- `code` (`` `code` ``)\n" +
        //                 "- Links: [Example](http://example.com)\n" +
        //                 "- Headers with # or ## etc.\n\n" +
        //                 "Try sending a message with Markdown format!");
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

                try {
                    Base64FileHandler.writeBase64ToFile(encodedString, fileName);
                    sendMarkdownMessage("✅ The file `" + fileName );
                } catch (IOException e) {
                    sendMarkdownMessage("❌ Error saving the file: `" + e.getMessage() + "`");
                }

                if (mimeType.startsWith("image/")) {
                    String img = "<img src='data:image/png;base64, " + encodedString + "' width='200px' />";
                    mediaContainer.add(new Html(img), new H3(fileName), new Hr());

                    sendMarkdownMessage("**Image uploaded:** " + fileName);
                } else if (mimeType.startsWith("audio/")) {
                    String audio = "<audio controls><source src='data:audio/mpeg;base64," + encodedString
                            + "' type='audio/mpeg'></audio>";
                    mediaContainer.add(new Html(audio), new H3(fileName), new Hr());

                    sendMarkdownMessage("**Audio file uploaded:** " + fileName);
                }

                sendMarkdownMessage("✅ The file `" + fileName );

            } catch (IOException e) {
                sendMarkdownMessage("❌ Error processing the file: `" + e.getMessage() + "`");
            }
        });

        upload.addFailedListener(event -> sendMarkdownMessage("❌ Upload failed: `" + event.getReason() + "`"));
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