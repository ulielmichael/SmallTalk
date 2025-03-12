package mu.smalltalk.Services;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * A broadcaster service that handles sending messages to all connected UIs.
 * This implementation ensures that messages are properly delivered to all
 * connected clients, including handling automatic page refreshes.
 */
public class GlobalMessageBroadcaster {
    // Using a single thread executor to ensure messages are processed in order
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    // Using a thread-safe map to store UI instances and their message listeners
    private static final ConcurrentHashMap<UI, List<Consumer<String>>> listeners = new ConcurrentHashMap<>();

    /**
     * Register a new UI instance and message listener
     * 
     * @param ui The Vaadin UI instance
     * @param listener The message consumer that will handle incoming messages
     * @return A Registration object that can be used to unregister
     */
    public static synchronized Registration register(UI ui, Consumer<String> listener) {
        if (ui == null) {
            throw new IllegalArgumentException("UI cannot be null");
        }
        
        // Get or create a list of listeners for this UI
        List<Consumer<String>> uiListeners = listeners.computeIfAbsent(ui, 
                k -> new CopyOnWriteArrayList<>());
        
        // Add the new listener
        uiListeners.add(listener);
        
        System.out.println("Registered UI: " + ui.getUIId() + " - Total UIs: " + listeners.size());
        
        // Return a registration that can be used to unregister this listener
        return () -> {
            synchronized (GlobalMessageBroadcaster.class) {
                List<Consumer<String>> currentListeners = listeners.get(ui);
                if (currentListeners != null) {
                    currentListeners.remove(listener);
                    
                    // If this UI has no more listeners, remove it completely
                    if (currentListeners.isEmpty()) {
                        listeners.remove(ui);
                    }
                }
                System.out.println("Unregistered listener from UI: " + ui.getUIId() + 
                        " - Total UIs: " + listeners.size());
            }
        };
    }

    /**
     * Broadcast a message to all registered UIs
     * 
     * @param message The message to broadcast
     */
    public static void broadcast(String message) {
        if (message == null) {
            return; // Don't broadcast null messages
        }
        
        System.out.println("Broadcasting message to " + listeners.size() + " UIs");
        
        // Execute broadcasting in a separate thread to avoid blocking
        executorService.execute(() -> {
            listeners.forEach((ui, uiListeners) -> {
                // For each UI, access its thread to update the UI safely
                try {
                    ui.access(() -> {
                        System.out.println("Sending message to UI: " + ui.getUIId());
                        
                        // Notify all listeners for this UI
                        uiListeners.forEach(listener -> {
                            try {
                                listener.accept(message);
                            } catch (Exception e) {
                                System.err.println("Error delivering message to listener in UI " + 
                                        ui.getUIId() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                        
                        // Push changes to the client to ensure immediate updates
                        try {
                            ui.push();
                        } catch (Exception e) {
                            System.err.println("Error pushing updates to UI " + ui.getUIId() + 
                                    ": " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Error accessing UI " + ui.getUIId() + ": " + e.getMessage());
                    e.printStackTrace();
                    
                    // Remove dead UIs to avoid future errors
                    listeners.remove(ui);
                }
            });
        });
    }
    
    /**
     * Check if a UI is currently registered
     * 
     * @param ui The UI to check
     * @return true if the UI is registered, false otherwise
     */
    public static synchronized boolean isRegistered(UI ui) {
        return listeners.containsKey(ui);
    }
    
    /**
     * Get the number of registered UIs
     * 
     * @return The number of registered UIs
     */
    public static synchronized int getRegisteredUICount() {
        return listeners.size();
    }
    
    /**
     * Get the total number of listeners across all UIs
     * 
     * @return The total number of listeners
     */
    public static synchronized int getTotalListenerCount() {
        return listeners.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * Clean up resources when the application shuts down
     */
    public static void shutdown() {
        executorService.shutdown();
    }
}