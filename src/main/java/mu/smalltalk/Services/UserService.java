package mu.smalltalk.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import mu.smalltalk.entitis.User;
import mu.smalltalk.repositoriy.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private User currentUser;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        System.out.println("UserService initialized with repository: " + userRepository);
    }
    
    public User registerUser(String fullName, String email, String password) {
        // Log debug information
        System.out.println("Registering user: " + fullName + " with email: " + email);
        
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
            System.out.println("Registration failed: Email already exists: " + email);
            throw new RuntimeException("Email already registered");
        }
        
        try {
            // Hash the password
            String hashedPassword = passwordEncoder.encode(password);
            System.out.println("Password hashed successfully");
            
            // Create a new user
            User user = new User(fullName, email, hashedPassword);
            System.out.println("User object created: " + user);
            
            // Save the user to the database
            User savedUser = userRepository.save(user);
            System.out.println("User saved successfully with ID: " + savedUser.getId());
            
            return savedUser;
        } catch (Exception e) {
            System.err.println("Error during user registration: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }
    
    public User authenticateUser(String email, String password) {
        System.out.println("Authenticating user with email: " + email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            System.out.println("User found: " + user);
            
            // Check if password matches
            if (passwordEncoder.matches(password, user.getPassword())) {
                System.out.println("Password matched for user: " + email);
                
                // Update last login time
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                
                // Set the current authenticated user
                this.currentUser = user;
                return user;
            } else {
                System.out.println("Password did not match for user: " + email);
            }
        } else {
            System.out.println("No user found with email: " + email);
        }
        
        throw new RuntimeException("Invalid email or password");
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public void logout() {
        this.currentUser = null;
    }
    
    // Method to update user's profile picture
    public void updateProfilePicture(User user) {
        if (user != null && user.getId() != null) {
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
                user.setProfilePic(profilePicData);
                userRepository.save(user);
            } else {
                throw new RuntimeException("User not found with ID: " + userId);
            }
        } else {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }
    
    // מתודה נוספת שמאפשרת חיפוש לפי אימייל ועדכון תמונת פרופיל
    public void updateProfilePictureByEmail(String email, String profilePicData) {
        if (email != null && !email.trim().isEmpty()) {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setProfilePic(profilePicData);
                userRepository.save(user);
            } else {
                throw new RuntimeException("User not found with email: " + email);
            }
        } else {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
    }
}