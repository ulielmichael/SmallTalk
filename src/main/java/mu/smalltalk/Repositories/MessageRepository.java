package mu.smalltalk.Repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

import mu.smalltalk.entitis.Message;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    // ================== בסיסיות מהירות ==================
    
    /**
     * Find messages by chat ID with pagination - מהיר עם אינדקס
     * השתמש בזה לטעינה רגילה
     */
    List<Message> findByChatId(String chatId, Pageable pageable);

    /**
     * Count messages by chat ID - מהיר עם אינדקס
     */
    long countByChatId(String chatId);

    // ================== קווריז מותאמים לביצועים גבוהים ==================

    /**
     * טעינת הודעות אחרונות מהר - עם אינדקס על chatId + time
     */
    @Query(value = "{'chatId': ?0}", 
           sort = "{'time': -1}")
    List<Message> findByChatIdOrderByTimeDesc(String chatId, Pageable pageable);

    /**
     * טעינת הודעות בסדר כרונולוגי - עם אינדקס על chatId + time
     */
    @Query(value = "{'chatId': ?0}", 
           sort = "{'time': 1}")
    List<Message> findByChatIdOrderByTimeAsc(String chatId, Pageable pageable);

    /**
     * Count מותאם לביצועים - מהיר מאוד עם אינדקס
     */
    @Query(value = "{'chatId': ?0}", count = true)
    long countByChatIdOptimized(String chatId);

    // ================== Projection Queries - טוען רק שדות נחוצים ==================

    /**
     * טעינה עם projection - חוסך bandwidth ו-memory
     * טוען רק את השדות הנחוצים לתצוגה
     */
    @Query(value = "{'chatId': ?0}", 
           fields = "{'textContent': 1, 'mediaContent': 1, 'mediaContentType': 1, 'time': 1, 'senderID': 1}",
           sort = "{'time': 1}")
    List<Message> findByChatIdProjectedSorted(String chatId);

    /**
     * טעינה עם projection ו-pagination - הכי מהיר לשיחות גדולות
     */
    @Query(value = "{'chatId': ?0}", 
           fields = "{'textContent': 1, 'mediaContent': 1, 'mediaContentType': 1, 'time': 1, 'senderID': 1}")
    List<Message> findByChatIdProjected(String chatId, Pageable pageable);

    /**
     * טעינת הודעות אחרונות עם projection - מהיר במיוחד
     */
    @Query(value = "{'chatId': ?0}", 
           fields = "{'textContent': 1, 'mediaContent': 1, 'mediaContentType': 1, 'time': 1, 'senderID': 1}",
           sort = "{'time': -1}")
    List<Message> findLatestByChatIdProjected(String chatId, Pageable pageable);

    // ================== Range Queries - לטעינה חכמה ==================

    /**
     * טעינה לפי טווח זמן - מהיר עם אינדקס מורכב
     */
    @Query("{'chatId': ?0, 'time': {'$gte': ?1, '$lte': ?2}}")
    List<Message> findByChatIdAndTimeRange(String chatId, long startTime, long endTime);

    /**
     * טעינת הודעות חדשות מזמן מסוים
     */
    @Query(value = "{'chatId': ?0, 'time': {'$gt': ?1}}", 
           sort = "{'time': 1}")
    List<Message> findByChatIdAfterTime(String chatId, long afterTime);

    /**
     * טעינת הודעות עד זמן מסוים
     */
    @Query(value = "{'chatId': ?0, 'time': {'$lt': ?1}}", 
           sort = "{'time': -1}")
    List<Message> findByChatIdBeforeTime(String chatId, long beforeTime);

    // ================== Lightweight Queries - לבדיקות מהירות ==================

    /**
     * בדיקה מהירה אם יש הודעות
     */
    @Query(value = "{'chatId': ?0}", count = true)
    boolean existsByChatId(String chatId);

    /**
     * קבלת הודעה אחרונה בלבד
     */
    @Query(value = "{'chatId': ?0}", 
           fields = "{'textContent': 1, 'time': 1}",
           sort = "{'time': -1}")
    Message findLatestByChatId(String chatId);

    /**
     * קבלת זמן ההודעה האחרונה
     */
    @Query(value = "{'chatId': ?0}", 
           fields = "{'time': 1}",
           sort = "{'time': -1}")
    Message findLatestTimeByChatId(String chatId);

    // ================== Aggregation Queries - לסטטיסטיקות מהירות ==================

    /**
     * ספירת הודעות לפי סוג תוכן
     */
    @Aggregation(pipeline = {
        "{ '$match': { 'chatId': ?0 } }",
        "{ '$group': { '_id': { '$cond': [{ '$ifNull': ['$textContent', false] }, 'text', 'media'] }, 'count': { '$sum': 1 } } }"
    })
    List<Object> countMessageTypesByChatId(String chatId);

    /**
     * קבלת סטטיסטיקות בסיסיות על השיחה
     */
    @Aggregation(pipeline = {
        "{ '$match': { 'chatId': ?0 } }",
        "{ '$group': { '_id': '$chatId', 'count': { '$sum': 1 }, 'firstMessage': { '$min': '$time' }, 'lastMessage': { '$max': '$time' } } }"
    })
    Object getChatStatistics(String chatId);

    // ================== Batch Operations - לעדכונים מהירים ==================

    /**
     * מחיקת הודעות ישנות לפי זמן
     */
    @Query(value = "{'chatId': ?0, 'time': {'$lt': ?1}}", delete = true)
    long deleteByChatIdAndTimeBefore(String chatId, long beforeTime);

    // ================== מתודות נוספות עם אינדקסים ==================

    /**
     * טעינה לפי שולח - עם אינדקס על senderID
     */
    @Query(value = "{'chatId': ?0, 'senderID': ?1}", 
           sort = "{'time': 1}")
    List<Message> findByChatIdAndSender(String chatId, String senderID, Pageable pageable);

    /**
     * חיפוש בתוכן הודעות - עם text index
     */
    @Query("{'chatId': ?0, '$text': {'$search': ?1}}")
    List<Message> searchInChatMessages(String chatId, String searchText, Pageable pageable);

    // ================== מתודות ישנות (נשמרות לתאימות) ==================
    
    List<Message> findByChatId(String chatId);
    List<Message> findBySenderID(String senderID);
    List<Message> findByReceiverID(String receiverID);
    List<Message> findBySenderIDOrReceiverID(String senderID, String receiverID);
}