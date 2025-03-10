package mu.smalltalk;

import java.time.LocalDateTime;

public class Message 
{
    private String senderID;
    private String receiverID;
    private byte[] textContent;
    private byte[] mediaContent;            // image/sound encrypted file bytes 
    private String mediaContentType;        // IMAGE/SOUND
    private String chatId;
    private LocalDateTime time;

    

    public Message() {}

    // public Message(String sender, String receiver, byte[] textContent, String chatId) 
    // {
    //     this.senderID = sender;
    //     this.receiverID = receiver;
    //     this.textContent = textContent;
    //     this.chatId = chatId;
    //     this.time = LocalDateTime.now();
    // }

    /**
     * 
     * @param sender
     * @param receiver
     * @param textContent
     * @param mediaContent
     * @param mediaContentType
     * @param chatId
     */
    public Message(String sender, String receiver, byte[] textContent, byte[] mediaContent, String mediaContentType, String chatId) 
    {
        this.senderID = sender;
        this.receiverID = receiver;
        this.textContent = textContent;
        this.mediaContent = mediaContent;
        this.mediaContentType = mediaContentType;
        this.chatId = chatId;
        this.time = LocalDateTime.now();
    }
 

    public String getSenderID() {
        return senderID;
    }

    public String getReceiverID() {
        return receiverID;
    }
   
    public String getChatId() {
        return chatId;
    }

    public LocalDateTime getTimestamp() {
        return time;
    }

    public void setSenderID(String sender) {
        this.senderID = sender;
    }

    public void setReceiverID(String receiver) {
        this.receiverID = receiver;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public byte[] getTextContent() {
        return textContent;
    }

    public void setTextContent(byte[] textContent) {
        this.textContent = textContent;
    }

    public byte[] getMediaContent() {
        return mediaContent;
    }

    public void setMediaContent(byte[] mediaContent) {
        this.mediaContent = mediaContent;
    }

    public String getMediaContentType() {
        return mediaContentType;
    }

    public void setMediaContentType(String mediaContentType) {
        this.mediaContentType = mediaContentType;
    }

    

}