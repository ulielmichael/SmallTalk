package mu.smalltalk.Services;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class GlobalMessageBroadcaster {
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final ConcurrentHashMap<Integer, Consumer<String>> listeners = new ConcurrentHashMap<>();
    
    public static synchronized Registration register(UI ui, Consumer<String> listener) {
        int uiId = ui.hashCode();
        listeners.put(uiId, listener);
        
        return () -> {
            synchronized (GlobalMessageBroadcaster.class) {
                listeners.remove(uiId);
            }
        };
    }
    
    public static synchronized void broadcast(String message) {
        executorService.execute(() -> {
            listeners.values().forEach(listener -> {
                try {
                    listener.accept(message);
                } catch (Exception e) {
                    System.err.println("Error broadcasting message: " + e.getMessage());
                }
            });
        });
    }
}
