package mu.smalltalk.entitis;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "users")
public class User {
    
    @Id  
    private String email;  // Email משמש כ-ID ראשי
    
    private String fullName;
    
    private String password;

    // קונסטרקטור ריק
    public User() {  }

    // קונסטרקטור עם פרמטרים - שימו לב לסדר!
    // הסדר צריך להיות: fullName, email, password (כמו שאתה קורא ב-UserService)
    public User(String fullName, String email, String password) {
        this.fullName = fullName;  // ראשון
        this.email = email;        // שני
        this.password = password;  // שלישי
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    // הוספת getId() שמחזיר את ה-email (כי זה ה-ID)
    public String getId() {
        return email;
    }
    
    public void setId(String id) {
        this.email = id;
    }

    @Override
    public String toString() {
        return "User [email=" + email + ", fullName=" + fullName + "]";
    }
}