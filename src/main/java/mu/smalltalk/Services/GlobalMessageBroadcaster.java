package mu.smalltalk.Services;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class GlobalMessageBroadcaster {
    private static final Map<UI, List<Consumer<String>>> listeners = new ConcurrentHashMap<>();
    
    public static synchronized Registration register(UI ui, Consumer<String> consumer) {
        listeners.computeIfAbsent(ui, k -> new CopyOnWriteArrayList<>()).add(consumer);
        
        return () -> unregister(ui, consumer);
    }
    
    public static synchronized void unregister(UI ui, Consumer<String> consumer) {
        listeners.computeIfPresent(ui, (key, list) -> {
            list.remove(consumer);
            return list.isEmpty() ? null : list;
        });
    }
    
    public static void broadcast(String message) {
        listeners.forEach((ui, consumerList) -> {
            if (ui.isAttached()) {
                ui.access(() -> {
                    consumerList.forEach(consumer -> consumer.accept(message));
                });
            } else {
                listeners.remove(ui);
            }
        });
    }
    
    public static int getActiveListenersCount() {
        return (int) listeners.keySet().stream()
                .filter(UI::isAttached)
                .count();
    }
}