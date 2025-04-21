// package mu.smalltalk.Services;



// import mu.smalltalk.User;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.stereotype.Service;

// import java.security.SecureRandom;
// import java.time.LocalDateTime;
// import java.util.Base64;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.UUID;

// @Service
// public class AuthenticationService {
//     private final Map<String, User> users = new HashMap<>(); // יוחלף במסד נתונים
//     private final Map<String, String> sessions = new HashMap<>(); // מזהה סשן -> מזהה משתמש
//     private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
//     /**
//      * רישום משתמש חדש
//      */
//     public User registerUser(String username, String password, byte[] publicKey) {
//         // בדיקת תקינות שם משתמש
//         if (username == null || username.trim().isEmpty()) {
//             throw new IllegalArgumentException("שם משתמש לא יכול להיות ריק");
//         }
        
//         // בדיקה האם המשתמש כבר קיים
//         if (findUserByUsername(username) != null) {
//             throw new IllegalArgumentException("שם המשתמש כבר תפוס");
//         }
        
//         // הצפנת הסיסמה
//         String passwordHash = passwordEncoder.encode(password);
        
//         // יצירת משתמש חדש
//         User newUser = new User(username, passwordHash, publicKey);
        
//         // שמירת המשתמש
//         users.put(newUser.getId(), newUser);
        
//         return newUser;
//     }
    
//     /**
//      * התחברות משתמש
//      */
//     public String login(String username, String password) {
//         User user = findUserByUsername(username);
        
//         if (user == null) {
//             throw new IllegalArgumentException("שם משתמש או סיסמה שגויים");
//         }
        
//         if (!passwordEncoder.matches(password, user.getPasswordHash())) {
//             throw new IllegalArgumentException("שם משתמש או סיסמה שגויים");
//         }
        
//         // יצירת מזהה סשן
//         String sessionId = generateSessionId();
        
//         // שמירת הסשן
//         sessions.put(sessionId, user.getId());
        
//         // עדכון זמן פעילות אחרון
//         user.updateLastActivity();
        
//         return sessionId;
//     }
    
//     /**
//      * בדיקת תקינות סשן
//      */
//     public User validateSession(String sessionId) {
//         String userId = sessions.get(sessionId);
        
//         if (userId == null) {
//             return null;
//         }
        
//         User user = users.get(userId);
        
//         if (user != null) {
//             user.updateLastActivity();
//         }
        
//         return user;
//     }
    
//     /**
//      * התנתקות משתמש
//      */
//     public void logout(String sessionId) {
//         sessions.remove(sessionId);
//     }
    
//     /**
//      * מציאת משתמש לפי שם משתמש
//      */
//     public User findUserByUsername(String username) {
//         return users.values().stream()
//                 .filter(u -> u.getUsername().equals(username))
//                 .findFirst()
//                 .orElse(null);
//     }
    
//     /**
//      * מציאת משתמש לפי מזהה
//      */
//     public User findUserById(String userId) {
//         return users.get(userId);
//     }
    
//     /**
//      * יצירת מזהה סשן אקראי
//      */
//     private String generateSessionId() {
//         SecureRandom random = new SecureRandom();
//         byte[] bytes = new byte[32];
//         random.nextBytes(bytes);
//         return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
//     }
    
//     /**
//      * שינוי סיסמה למשתמש
//      */
//     public void changePassword(String userId, String oldPassword, String newPassword) {
//         User user = users.get(userId);
        
//         if (user == null) {
//             throw new IllegalArgumentException("משתמש לא נמצא");
//         }
        
//         if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
//             throw new IllegalArgumentException("הסיסמה הישנה שגויה");
//         }
        
//         user.setPasswordHash(passwordEncoder.encode(newPassword));
//     }
    
//     /**
//      * עדכון מפתח ציבורי למשתמש
//      */
//     public void updatePublicKey(String userId, byte[] newPublicKey) {
//         User user = users.get(userId);
        
//         if (user == null) {
//             throw new IllegalArgumentException("משתמש לא נמצא");
//         }
        
//         user.setPublicKey(newPublicKey);
//     }
// }