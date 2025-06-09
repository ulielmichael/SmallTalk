package mu.smalltalk.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import mu.smalltalk.entitis.User;
import mu.smalltalk.Repositories.UserRepository;

import com.vaadin.flow.server.VaadinSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static UserRepository userRepository = null;
    private final BCryptPasswordEncoder passwordEncoder;
    
    // קבועים לשימוש בניהול השיחה
    public static final String USER_KEY = "user";
    public static final String USERNAME_KEY = "username";
    
    @Autowired
    public UserService(UserRepository userRepository) {
        UserService.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        // System.out.println("UserService initialized with repository: " + userRepository);
    }
    
    /**
     * בדיקה האם המשתמש הנוכחי מחובר למערכת
     */
    public static boolean isUserAuthenticated() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null && session.getAttribute(USER_KEY) != null;
    }
    
    /**
     * החזרת משתמש מאומת נוכחי
     */
    public static User getAuthenticatedUser() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(USER_KEY);
    }
    
    /**
     * ניקוי פרטי המשתמש מהסשן (התנתקות)
     */
    public static void clearAuthenticatedUser() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_KEY, null);
            session.setAttribute(USERNAME_KEY, null);
        }
    }
    
    public User registerUser(String fullName, String email, String password) {
        // Log debug information
        // System.out.println("Registering user: " + fullName + " with email: " + email);
        
        // Validate inputs
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        // Check if email already exists - קריטי לבצע בדיקה זו
        if (userRepository.existsByEmail(email)) {
            // System.out.println("Registration failed: Email already exists: " + email);
            throw new RuntimeException("Email already registered");
        }
        
        try {
            // Hash the password
            String hashedPassword = passwordEncoder.encode(password);
            // System.out.println("Password hashed successfully for user: " + email);
            
            // Create a new user
            User user = new User(fullName, email, hashedPassword);
            // System.out.println("User object created: " + user);
            
            // Save the user to the database
            User savedUser = userRepository.save(user);
            // System.out.println("User saved successfully with email: " + savedUser.getEmail());
            
            return savedUser;
        } catch (Exception e) {
            // System.err.println("Error during user registration: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }
    public User authenticateUser(String email, String password) {
    // System.out.println("Authenticating user with email: " + email);
    
    if (email == null || email.trim().isEmpty()) {
        throw new IllegalArgumentException("Email cannot be null or empty");
    }
    
    if (password == null || password.trim().isEmpty()) {
        throw new IllegalArgumentException("Password cannot be null or empty");
    }
    
    try {
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // System.out.println("User found in database: " + user.getEmail());
            
            String storedPassword = user.getPassword();
            boolean passwordMatches = false;
            
            // בדיקה אם הסיסמה המאוחסנת היא BCrypt hash
            if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                // סיסמה מוצפנת - השתמש ב-BCrypt
                passwordMatches = passwordEncoder.matches(password, storedPassword);
                // System.out.println("Using BCrypt validation");
            } else {
                // סיסמה לא מוצפנת (legacy) - השוואה ישירה
                passwordMatches = password.equals(storedPassword);
                // System.out.println("Using plain text validation (legacy)");
                
                // אופציונלי: עדכן לסיסמה מוצפנת
                if (passwordMatches) {
                    String hashedPassword = passwordEncoder.encode(password);
                    user.setPassword(hashedPassword);
                    userRepository.save(user);
                    System.out.println("Updated password to BCrypt hash for user: " + email);
                }
            }
            
            if (passwordMatches) {
                // System.out.println("Password matched for user: " + email);
                
                // Set the current authenticated user
                VaadinSession session = VaadinSession.getCurrent();
                session.setAttribute(USER_KEY, user);
                session.setAttribute(USERNAME_KEY, user.getEmail());
                
                return user;
            } else {
                // System.out.println("Password did not match for user: " + email);
                // System.out.println("Provided password: " + password);
                // System.out.println("Stored hash: " + storedPassword);
            }
        } else {
            // System.out.println("No user found with email: " + email);
            
            // Debug: הדפס את כל המשתמשים כדי לבדוק מה יש במסד הנתונים
            List<User> allUsers = getAllUsers();
            // System.out.println("Total users in database: " + allUsers.size());
            for (User u : allUsers) {
                // System.out.println("  - User: " + u.getEmail() + " (ID: " + u.getEmail() + ")");
            }
        }
    } catch (Exception e) {
        // System.err.println("Error during authentication: " + e.getMessage());
        e.printStackTrace();
    }
    
    throw new RuntimeException("Invalid email or password");
}
    
    
    public User getCurrentUser() {
        return getAuthenticatedUser();
    }
    
    public void logout() {
        clearAuthenticatedUser();
    }
    
    // Method to update user's profile picture
    public void updateProfilePicture(User user) {
        if (user != null && user.getEmail() != null) {
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("User or ID cannot be null");
        }
    }
    
    // Alternative method if you prefer to pass ID and image data separately
    public void updateProfilePicture(String userId, String profilePicData) {
        if (userId != null && !userId.trim().isEmpty()) {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                // user.setProfilePic(profilePicData);
                userRepository.save(user);
            } else {
                throw new RuntimeException("User not found with ID: " + userId);
            }
        } else {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }
    
    public void updateProfilePictureByEmail(String email, String profilePicData) {
        if (email != null && !email.trim().isEmpty()) {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                // user.setProfilePic(profilePicData);
                userRepository.save(user);
            } else {
                throw new RuntimeException("User not found with email: " + email);
            }
        } else {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
    }

 public static User getUserById(String email) {
    if (email == null || email.isEmpty()) {
        return null;
    }
    // תשתמש ב-findByEmail במקום findById
    Optional<User> userOptional = userRepository.findByEmail(email);
    return userOptional.orElse(null);
}

public static User getUserByEmail(String email) {
    if (email == null || email.isEmpty()) {
        return null;
    }
    // תיקון: תשתמש ב-findByEmail במקום findById
    Optional<User> userOptional = userRepository.findByEmail(email);
    return userOptional.orElse(null);
}

// גם תוסיף פונקציה לניקוי מסד הנתונים אם צריך למחוק משתמשים שגויים
public void deleteUserByEmail(String email) {
    Optional<User> userOptional = userRepository.findByEmail(email);
    if (userOptional.isPresent()) {
        userRepository.delete(userOptional.get());
        // System.out.println("Deleted user: " + email);
    }
}

        

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }
}