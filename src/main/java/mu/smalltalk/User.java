// package mu.smalltalk;
// import org.springframework.data.annotation.Id;
// import org.springframework.data.mongodb.core.mapping.Document;

// import java.time.LocalDateTime;
// import java.util.UUID;
// @Document(collection = "users")
// public class User {
//     private String id;
//     private String username;
//     private String passwordHash;
//     private byte[] publicKey;        // המפתח הציבורי שמשתמש לקבלת הודעות
//     private LocalDateTime registrationDate;
//     private LocalDateTime lastActivity;
//     private boolean isActive;
//     private String profilePicture;

//     public User() {
//         this.id = UUID.randomUUID().toString();
//         this.registrationDate = LocalDateTime.now();
//         this.isActive = true;
//     }

//     public User(String username, String passwordHash, byte[] publicKey) {
//         this();
//         this.username = username;
//         this.passwordHash = passwordHash;
//         this.publicKey = publicKey;
//     }

//     // Getters
//     public String getId() {
//         return id;
//     }

//     public String getUsername() {
//         return username;
//     }

//     public String getPasswordHash() {
//         return passwordHash;
//     }

//     public byte[] getPublicKey() {
//         return publicKey;
//     }

//     public LocalDateTime getRegistrationDate() {
//         return registrationDate;
//     }

//     public LocalDateTime getLastActivity() {
//         return lastActivity;
//     }

//     public boolean isActive() {
//         return isActive;
//     }

//     public String getProfilePicture() {
//         return profilePicture;
//     }

//     // Setters
//     public void setId(String id) {
//         this.id = id;
//     }

//     public void setUsername(String username) {
//         this.username = username;
//     }

//     public void setPasswordHash(String passwordHash) {
//         this.passwordHash = passwordHash;
//     }

//     public void setPublicKey(byte[] publicKey) {
//         this.publicKey = publicKey;
//     }

//     public void setRegistrationDate(LocalDateTime registrationDate) {
//         this.registrationDate = registrationDate;
//     }

//     public void setLastActivity(LocalDateTime lastActivity) {
//         this.lastActivity = lastActivity;
//     }

//     public void setActive(boolean active) {
//         isActive = active;
//     }

//     public void setProfilePicture(String profilePicture) {
//         this.profilePicture = profilePicture;
//     }

//     public void updateLastActivity() {
//         this.lastActivity = LocalDateTime.now();
//     }
// }