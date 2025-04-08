package mu.smalltalk.Services;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.shared.Registration;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import mu.Chatstorage;

public class GlobalMessageBroadcaster {
    private static final List<BroadcastListener> listeners = new CopyOnWriteArrayList<>();
    
    // מחלקה פנימית לשמירת מידע על המאזין
    private static class BroadcastListener {
        final UI ui;
        final Consumer<String> consumer;
        
        BroadcastListener(UI ui, Consumer<String> consumer) {
            this.ui = ui;
            this.consumer = consumer;
        }
    }
    
    public static synchronized Registration register(UI ui, Consumer<String> messageConsumer) {
        if (ui == null || messageConsumer == null) {
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
    
    public static synchronized void broadcast(String message) {
        // לוג - לבדיקה
        System.out.println("Broadcasting message: " + message);
        System.out.println("Active listeners: " + listeners.size());
        
        // שמירה במאגר
        Chatstorage.addMessage(message);
        
        // שידור לכל המאזינים
        listeners.forEach(listener -> {
            UI ui = listener.ui;
            Consumer<String> consumer = listener.consumer;
            
            if (ui != null && ui.isAttached()) {
                try {
                    System.out.println("Sending to UI: " + ui.getUIId());
                    ui.access(() -> {
                        try {
                            consumer.accept(message);
                            ui.push(); // דחיפה מיידית לדפדפן
                        } catch (Exception e) {
                            System.err.println("Error delivering message to UI " + ui.getUIId() + ": " + e.getMessage());
                        }
                    });
                } catch (UIDetachedException e) {
                    System.out.println("UI " + ui.getUIId() + " detached, removing listener");
                    listeners.remove(listener);
                }
            } else {
                System.out.println("UI detached or null, removing listener");
                listeners.remove(listener);
            }
        });
    }
}