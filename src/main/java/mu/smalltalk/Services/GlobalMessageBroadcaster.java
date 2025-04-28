package mu.smalltalk.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.shared.Registration;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import mu.smalltalk.Chatstorage;

public class GlobalMessageBroadcaster {
    private static final List<BroadcastListener> listeners = new CopyOnWriteArrayList<>();
    
    private static class BroadcastListener {
        final UI ui;
        final Consumer<String> consumer;
        
        BroadcastListener(UI ui, Consumer<String> consumer) {
            this.ui = ui;
            this.consumer = consumer;
        }
    }
    
    /**
     * Register a UI and message consumer to receive broadcasts
     * @param ui The UI instance to register
     * @param messageConsumer Consumer that processes received messages
     * @return Registration object that can be used to unregister
     */
    public static synchronized Registration register(UI ui, Consumer<String> messageConsumer) {
        if (ui == null || messageConsumer == null) {
            System.err.println("Cannot register null UI or consumer");
            return null;
        }
        
        System.out.println("Registering UI: " + ui.getUIId() + " for broadcasts");
        BroadcastListener listener = new BroadcastListener(ui, messageConsumer);
        listeners.add(listener);
        
        return () -> {
            System.out.println("Removing UI: " + ui.getUIId() + " from broadcasts");
            listeners.remove(listener);
        };
    }
    
    /**
     * Broadcast a message to all registered listeners
     * @param message The message to broadcast
     */
    public static synchronized void broadcast(String message) {
        // Store message in persistent storage
        Chatstorage.addMessage(message);
        
        System.out.println("Broadcasting message: " + message);
        System.out.println("Active listeners: " + listeners.size());
        
        List<BroadcastListener> toRemove = new ArrayList<>();
        
        for (BroadcastListener listener : listeners) {
            UI ui = listener.ui;
            Consumer<String> consumer = listener.consumer;
            
            if (ui != null && ui.isAttached()) {
                try {
                    System.out.println("Sending to UI: " + ui.getUIId());
                    ui.access(() -> {
                        try {
                            consumer.accept(message);
                            ui.push();
                        } catch (Exception e) {
                            System.err.println("Error delivering message to UI " + ui.getUIId() + ": " + e.getMessage());
                        }
                    });
                    
                    ui.getPage().executeJs(
                        "localStorage.setItem('chat-update-trigger', Date.now().toString());"
                    );
                } catch (UIDetachedException e) {
                    System.out.println("UI " + ui.getUIId() + " detached, marking for removal");
                    toRemove.add(listener);
                }
            } else {
                System.out.println("UI detached or null, marking for removal");
                toRemove.add(listener);
            }
        }
        
        if (!toRemove.isEmpty()) {
            listeners.removeAll(toRemove);
            System.out.println("Removed " + toRemove.size() + " detached listeners. Remaining: " + listeners.size());
        }
    }
        
    
    /**
     * Check if a UI is currently registered
     * @param ui The UI to check
     * @return true if registered, false otherwise
     */
    public static synchronized boolean isRegistered(UI ui) {
        if (ui == null) return false;
        
        for (BroadcastListener listener : listeners) {
            if (listener.ui.equals(ui)) {
                return true;
            }
        }
        return false;
    }
}