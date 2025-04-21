// package mu.smalltalk.Services;

// import mu.smalltalk.User;
// import mu.smalltalk.repository.UserRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service;

// @Service
// public class UserService {
    
//     private final UserService userRepository;
//     private final PasswordEncoder passwordEncoder;
    
//     @Autowired
//     public UserService(UserService userRepository, PasswordEncoder passwordEncoder) {
//         this.userRepository = userRepository;
//         this.passwordEncoder = passwordEncoder;
//     }
    
//     public User registerUser(String fullName, String email, String password) {
//         // Check if user already exists
//         if (userRepository.existsByEmail(email)) {
//             throw new RuntimeException("Email already in use");
//         }
        
//         // Create new user with encoded password
//         User newUser = new User(fullName, email, passwordEncoder.encode(password));
        
//         // Save to database
//         return userRepository.save(newUser);
//     }
// }