package mu.smalltalk.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import mu.smalltalk.Repositories.MessageRepository;
import mu.smalltalk.entitis.Message;

@Service
public class ChatService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private MessageService messageService;
    
    // הגדרות מותאמות לשיחות גדולות
    private static final int INITIAL_LOAD_SIZE = 20; // טוען רק 20 הודעות אחרונות בהתחלה
    private static final int PAGINATION_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100; // הקטנת הגבול המקסימלי
    private static final int CACHE_SIZE = 200; // מספר הודעות בקאש
    
    /**
     * הוספת הודעה עם אופטימיזציה
     */
    @CacheEvict(value = {"groupMessages", "latestMessages"}, key = "#groupId")
    public void addGroupMessage(String message, String groupId, String senderId) {
        try {
            byte[] messageBytes = message.getBytes();
            messageService.saveTextMessage(senderId, groupId, messageBytes, groupId);
        } catch (Exception e) {
            // System.err.println("Error adding group message: " + e.getMessage());
            throw new RuntimeException("Failed to save message", e);
        }
    }
    
    /**
     * טעינה ראשונית מהירה - רק הודעות אחרונות
     */
    @Cacheable(value = "latestMessages", key = "#groupId")
    public List<String> getInitialMessages(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // טוען רק הודעות אחרונות עם projection
            return loadLatestMessagesOptimized(groupId, INITIAL_LOAD_SIZE);
        } catch (Exception e) {
            // System.err.println("Error loading initial messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * טעינה מהירה עם projection - טוען רק שדות נחוצים
     */
    private List<String> loadLatestMessagesOptimized(String groupId, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            
            // שימוש בקוורי מותאם עם projection
            List<Message> messages = messageRepository.findByChatIdOrderByTimeDesc(groupId, pageable);
            
            if (messages == null || messages.isEmpty()) {
                return new ArrayList<>();
            }
            
            // המרה מהירה והיפוך סדר
            List<String> result = messages.stream()
                    .map(this::convertMessageToStringFast)
                    .filter(msg -> msg != null && !msg.isEmpty())
                    .collect(Collectors.toList());
            
            java.util.Collections.reverse(result);
            return result;
            
        } catch (Exception e) {
            // System.err.println("Error in optimized message loading: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * המרה מהירה של הודעה לטקסט
     */
    private String convertMessageToStringFast(Message message) {
        if (message == null) {
            return "";
        }
        
        try {
            // בדיקה מהירה של תוכן טקסט
            if (message.getTextContent() != null && message.getTextContent().length > 0) {
                return new String(message.getTextContent());
            }
            
            // אם יש מדיה - החזרת תיאור קצר
            if (message.getMediaContent() != null && message.getMediaContent().length > 0) {
                return "[" + (message.getMediaContentType() != null ? message.getMediaContentType() : "Media") + "]";
            }
            
            return "";
            
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * טעינת הודעות נוספות עם אופטימיזציה
     */
    public List<String> loadMoreMessages(String groupId, int page) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // התחלה מדף 1 (דף 0 הוא ההודעות הראשונות)
            Pageable pageable = PageRequest.of(page, PAGINATION_SIZE);
            
            // שימוש בקוורי מותאם
            List<Message> olderMessages = messageRepository.findByChatIdOrderByTimeAsc(groupId, pageable);
            
            if (olderMessages == null || olderMessages.isEmpty()) {
                return new ArrayList<>();
            }
            
            return olderMessages.stream()
                    .map(this::convertMessageToStringFast)
                    .filter(msg -> msg != null && !msg.isEmpty())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            // System.err.println("Error loading more messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * טעינה אסינכרונית מהירה
     */
    @Async
    public CompletableFuture<List<String>> loadMoreMessagesAsync(String groupId, int page) {
        try {
            List<String> messages = loadMoreMessages(groupId, page);
            return CompletableFuture.completedFuture(messages);
        } catch (Exception e) {
            // System.err.println("Error in async message loading: " + e.getMessage());
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }
    
    /**
     * טעינה אסינכרונית בסיסית - לתאימות עם קוד ישן
     */
    @Async
    public CompletableFuture<List<String>> loadMessagesAsync(String groupId) {
        try {
            List<String> messages = getInitialMessages(groupId);
            return CompletableFuture.completedFuture(messages);
        } catch (Exception e) {
            // System.err.println("Error in async message loading: " + e.getMessage());
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }
    
    /**
     * טעינה אסינכרונית עם גודל מותאם אישית
     */
    @Async
    public CompletableFuture<List<String>> loadMessagesAsync(String groupId, int size) {
        try {
            List<String> messages = loadLatestMessagesOptimized(groupId, size);
            return CompletableFuture.completedFuture(messages);
        } catch (Exception e) {
            // System.err.println("Error in async message loading: " + e.getMessage());
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }
    
    /**
     * ספירה מהירה עם קאש
     */
    @Cacheable(value = "messageCount", key = "#groupId")
    public long getMessageCount(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return 0;
        }
        
        try {
            return messageRepository.countByChatIdOptimized(groupId);
        } catch (Exception e) {
            // System.err.println("Error counting messages: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * טעינת חלק מההודעות לפי טווח זמן
     */
    public List<String> getMessagesByTimeRange(String groupId, long startTime, long endTime) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            List<Message> messages = messageRepository.findByChatIdAndTimeRange(groupId, startTime, endTime);
            
            return messages.stream()
                    .map(this::convertMessageToStringFast)
                    .filter(msg -> msg != null && !msg.isEmpty())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            // System.err.println("Error loading messages by time range: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * טעינה מהירה עם batch processing
     */
    public List<String> loadMessagesBatch(String groupId, int batchSize, int batchNumber) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        if (batchSize <= 0 || batchSize > MAX_PAGE_SIZE) {
            batchSize = PAGINATION_SIZE;
        }
        
        try {
            Pageable pageable = PageRequest.of(batchNumber, batchSize, Sort.by(Sort.Direction.ASC, "time"));
            
            List<Message> messages = messageRepository.findByChatIdProjectedSorted(groupId);
            
            // לקיחת הבאצ' הרצוי
            int startIndex = batchNumber * batchSize;
            int endIndex = Math.min(startIndex + batchSize, messages.size());
            
            if (startIndex >= messages.size()) {
                return new ArrayList<>();
            }
            
            return messages.subList(startIndex, endIndex).stream()
                    .map(this::convertMessageToStringFast)
                    .filter(msg -> msg != null && !msg.isEmpty())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            // System.err.println("Error in batch loading: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * ניקוי קאש מותאם
     */
    @CacheEvict(value = {"groupMessages", "latestMessages", "messageCount"}, key = "#groupId")
    public void clearGroupCache(String groupId) {
        // הקאש ינוקה אוטומטית
    }
    
    /**
     * ניקוי כל הקאש
     */
    @CacheEvict(value = {"groupMessages", "latestMessages", "messageCount"}, allEntries = true)
    public void clearAllCaches() {
        // הקאש ינוקה אוטומטית
    }
    
    /**
     * בדיקה מהירה אם יש הודעות
     */
    public boolean hasMessages(String groupId) {
        return getMessageCount(groupId) > 0;
    }
    
    /**
     * טעינה חכמה - טוען כמות משתנה לפי גודל השיחה
     */
    public List<String> getMessagesAdaptive(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            long totalMessages = getMessageCount(groupId);
            
            int loadSize;
            if (totalMessages < 100) {
                // שיחה קטנה - טוען הכל
                loadSize = (int) totalMessages;
            } else if (totalMessages < 1000) {
                // שיחה בינונית - טוען 50 אחרונות
                loadSize = 50;
            } else {
                // שיחה גדולה - טוען רק 20 אחרונות
                loadSize = INITIAL_LOAD_SIZE;
            }
            
            return loadLatestMessagesOptimized(groupId, loadSize);
            
        } catch (Exception e) {
            // System.err.println("Error in adaptive loading: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // ================== מתודות תאימות עם קוד ישן ==================
    
    /**
     * טעינת הודעות סטנדרטית - לתאימות עם קוד ישן
     */
    public List<String> getGroupMessages(String groupId) {
        return getInitialMessages(groupId);
    }
    
    /**
     * טעינת הודעות עם גודל מותאם - לתאימות עם קוד ישן
     */
    public List<String> getGroupMessages(String groupId, int pageSize) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        if (pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
            pageSize = PAGINATION_SIZE;
        }
        
        try {
            return loadLatestMessagesOptimized(groupId, pageSize);
        } catch (Exception e) {
            // System.err.println("Error loading group messages with pagination: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * טעינה אסינכרונית סטנדרטית - לתאימות עם קוד ישן
     */
    @Async
    public CompletableFuture<List<String>> getGroupMessagesAsync(String groupId) {
        try {
            List<String> messages = getInitialMessages(groupId);
            return CompletableFuture.completedFuture(messages);
        } catch (Exception e) {
            // System.err.println("Error in async message loading: " + e.getMessage());
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }
    
    /**
     * קבלת הודעות אחרונות - לתאימות עם קוד ישן
     */
    public List<String> getLatestMessages(String groupId, int count) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        if (count <= 0) {
            count = 10;
        }
        
        try {
            return loadLatestMessagesOptimized(groupId, count);
        } catch (Exception e) {
            // System.err.println("Error loading latest messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * טעינת הודעות נוספות עם offset - לתאימות עם קוד ישן
     */
    public List<String> loadMoreMessages(String groupId, int offset, int limit) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        if (limit <= 0 || limit > MAX_PAGE_SIZE) {
            limit = PAGINATION_SIZE;
        }
        
        try {
            int page = Math.max(0, offset / limit);
            return loadMoreMessages(groupId, page);
        } catch (Exception e) {
            // System.err.println("Error loading more messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}