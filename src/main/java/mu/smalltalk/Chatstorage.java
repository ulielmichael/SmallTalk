package mu.smalltalk;



import java.util.ArrayList;
import java.util.List;

public class Chatstorage {
    private static final List<String> messages = new ArrayList<>();

    public static synchronized void addMessage(String message) {
        messages.add(message);
    }

    public static synchronized List<String> getMessages() {
        return new ArrayList<>(messages);
    }
}
