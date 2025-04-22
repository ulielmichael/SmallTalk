package mu.smalltalk.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import mu.smalltalk.User;
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
        
        // Check if email already exists
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
}