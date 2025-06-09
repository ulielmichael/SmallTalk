package mu.smalltalk.Pages;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;

import mu.smalltalk.Pages.Chatpage;
import mu.smalltalk.Services.UserService;
import mu.smalltalk.entitis.User;

import org.springframework.beans.factory.annotation.Autowired;

@CssImport("./styles/shared-styles.css")
@Route("/login")
public class LoginPage extends VerticalLayout {

    private final UserService userService;
    
    private EmailField emailField;
    private PasswordField passwordField;

    @Autowired
    public LoginPage(UserService userService) {
        this.userService = userService;
        
        // Set basic page properties
        addClassName("login-page");
        setSpacing(false);
        setMargin(false);
        setPadding(false);
        setSizeFull();
        getStyle().set("background-color", "#f5f7fa");
        
        // Add navigation bar at the top
        HorizontalLayout navbar = createNavigationBar();
        
        // Create the two-column layout
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setMargin(false);
        mainLayout.setSpacing(false);
        
        // Left column - Login form
        VerticalLayout leftColumn = createLoginFormColumn();
        
        // Right column - Image/Pattern
        VerticalLayout rightColumn = createImageColumn();
        
        // Add columns to main layout
        mainLayout.add(leftColumn, rightColumn);
        mainLayout.setFlexGrow(1, leftColumn);
        mainLayout.setFlexGrow(1, rightColumn);
        
        // Add navbar and main layout to the page
        add(navbar, mainLayout);
    }
    
    // Navigation bar method copied from HomePage
    private HorizontalLayout createNavigationBar() {
        HorizontalLayout navbar = new HorizontalLayout();
        navbar.setWidthFull();
        navbar.setHeight("64px");
        navbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        navbar.setAlignItems(Alignment.CENTER);
        navbar.getStyle()
            .set("background-color", "#ffffff")
            .set("padding", "0 24px")
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)")
            .set("position", "sticky")
            .set("top", "0")
            .set("z-index", "1000");
        
        // Logo with text - create horizontal container for logo and text
        HorizontalLayout logoContainer = new HorizontalLayout();
        logoContainer.setAlignItems(Alignment.CENTER);
        logoContainer.setSpacing(true);
        logoContainer.getStyle().set("cursor", "pointer");
        
        // Logo image
        Image logo = new Image("images/live-chat.png", "SmallTalk");
        logo.setHeight("40px");
        logo.setWidth("auto");
        
        // Logo text
        H3 logoText = new H3("SmallTalk");
        logoText.getStyle()
            .set("margin", "0")
            .set("color", "#2a5885")
            .set("font-weight", "bold")
            .set("font-size", "22px");
        
        // Add logo and text to container
        logoContainer.add(logo, logoText);
        
        // Add click to return to home page
        logoContainer.addClickListener(e -> logoContainer.getUI().ifPresent(ui -> ui.navigate("")));
        
        // Navigation links
        HorizontalLayout navLinks = new HorizontalLayout();
        navLinks.setSpacing(true);
        
        Anchor homeLink = new Anchor("", "Home");
        Anchor chatlink = new Anchor("chat", "Chat");
        Anchor loginLink = new Anchor("login", "Login");
        loginLink.getStyle().set("font-weight", "bold"); // Highlight the current page
        Anchor signupLink = new Anchor("signup", "Signup");
        Anchor featuresLink = new Anchor("#features", "Features");
        Anchor securityLink = new Anchor("#security", "Security");
        Anchor techLink = new Anchor("#technology", "Technology");
        Anchor aboutLink = new Anchor("#about", "About");
        
        navLinks.add(homeLink, featuresLink, securityLink, techLink, aboutLink, chatlink, loginLink,  signupLink);
        
        // Style all links
        for (int i = 0; i < navLinks.getComponentCount(); i++) {
            com.vaadin.flow.component.Component component = navLinks.getComponentAt(i);
            if (component instanceof Anchor) {
                ((Anchor) component).getStyle()
                    .set("color", "#444")
                    .set("text-decoration", "none")
                    .set("margin", "0 12px")
                    .set("padding", "6px 12px")
                    .set("border-radius", "4px")
                    .set("transition", "background-color 0.3s");
            }
        }
        
        // Add logo container and nav links to navbar
        navbar.add(logoContainer, navLinks);
        return navbar;
    }
    
    private VerticalLayout createLoginFormColumn() {
        VerticalLayout column = new VerticalLayout();
        column.addClassName("login-form-column");
        column.setJustifyContentMode(JustifyContentMode.CENTER);
        column.setAlignItems(Alignment.CENTER);
        column.setHeight("100%");
        column.getStyle()
            .set("padding", "6em 1em");
        
        // Form container to limit width
        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setMaxWidth("400px");
        formContainer.setWidth("100%");
        formContainer.setPadding(true);
        formContainer.setSpacing(true);
        
        // Logo/Icon
        Div logoContainer = new Div();
        logoContainer.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("align-items", "center")
            .set("gap", "12px")
            .set("margin-bottom", "2em")
            .set("width", "100%");
        
        Div iconCircle = new Div();
        iconCircle.getStyle()
            .set("width", "48px")
            .set("height", "48px")
            .set("border-radius", "12px")
            .set("background-color", "rgba(42, 88, 133, 0.1)")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center");
            
        Icon messageIcon = VaadinIcon.COMMENT.create();
        messageIcon.setSize("24px");
        messageIcon.setColor("#2a5885");
        iconCircle.add(messageIcon);
        
        H2 title = new H2("Welcome Back");
        title.getStyle()
            .set("margin", "8px 0 0 0")
            .set("font-size", "24px")
            .set("font-weight", "700");
        
        Paragraph subtitle = new Paragraph("Sign in to your account");
        subtitle.getStyle()
            .set("margin", "0")
            .set("color", "rgba(0, 0, 0, 0.6)")
            .set("font-size", "16px");
        
        logoContainer.add(iconCircle, title, subtitle);
        
        // Email field
        Div emailFieldWrapper = new Div();
        emailFieldWrapper.setWidthFull();
        emailFieldWrapper.getStyle().set("margin-bottom", "16px");
        
        H5 emailLabel = new H5("Email");
        emailLabel.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-weight", "500");
        
        emailField = new EmailField();
        emailField.setWidthFull();
        emailField.setPlaceholder("you@example.com");
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setRequired(true);
        emailField.setErrorMessage("Please enter a valid email address");
        
        emailFieldWrapper.add(emailLabel, emailField);
        
        // Password field
        Div passwordFieldWrapper = new Div();
        passwordFieldWrapper.setWidthFull();
        passwordFieldWrapper.getStyle().set("margin-bottom", "24px");
        
        H5 passwordLabel = new H5("Password");
        passwordLabel.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-weight", "500");
        
        passwordField = new PasswordField();
        passwordField.setWidthFull();
        passwordField.setPlaceholder("••••••••");
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setRequired(true);
        passwordField.setErrorMessage("Password is required");
        
        passwordFieldWrapper.add(passwordLabel, passwordField);
        
        // Login button
        Button loginButton = new Button("Sign in");
        loginButton.getStyle()
            .set("background-color", "#2a5885")
            .set("color", "white")
            .set("border-radius", "4px")
            .set("font-weight", "500")
            .set("cursor", "pointer");
        loginButton.setWidthFull();
        loginButton.setHeight("48px");
        
        // Add login button click handler
        loginButton.addClickListener(e -> {
            try {
                // Validate form
                if (!validateForm()) {
                    Notification.show("Please fill in all fields correctly", 
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                String email = emailField.getValue();
                String password = passwordField.getValue();
                
                // Log the login attempt (for debugging)
                // System.out.println("Login attempt for email: " + email);
                
                // Try to authenticate the user
                User authenticatedUser = userService.authenticateUser(email, password);
                
                // Additional null check
                if (authenticatedUser == null) {
                    Notification.show("Authentication failed: Invalid credentials", 
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    
                    // Optional: Add some logging
                    // System.err.println("Login failed for email: " + email);
                    return;
                }
                
                // Determine display name
                String displayName = authenticatedUser.getFullName() != null && !authenticatedUser.getFullName().isEmpty()
                    ? authenticatedUser.getFullName()
                    : authenticatedUser.getEmail();
                
                // Store session attributes
                VaadinSession session = VaadinSession.getCurrent();
                session.setAttribute("username", displayName);
                session.setAttribute("userEmail", authenticatedUser.getEmail());
                session.setAttribute("sessionId", null);
                
                // Store in localStorage for persistence
                UI.getCurrent().getPage().executeJs(
                    "localStorage.setItem('chat-session-id', $0);", displayName);
                
                // Successful login notification
                Notification.show("Login successful! Welcome, " + displayName,
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                // Navigate to chat page
                getUI().ifPresent(ui -> ui.navigate(Chatpage.class));
                
            } catch (Exception ex) {
                // Detailed error logging
                // System.err.println("Login error: " + ex.getMessage());
                ex.printStackTrace();
                
                // Show generic error message to user
                Notification.show("Login failed. Please check your credentials and try again.",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        // Sign up link
        Div signupLinkContainer = new Div();
        signupLinkContainer.getStyle()
            .set("display", "flex")
            .set("justify-content", "center")
            .set("margin-top", "24px")
            .set("width", "100%");
        
        Span signupText = new Span("Don't have an account? ");
        signupText.getStyle().set("color", "rgba(0, 0, 0, 0.6)");
        
        RouterLink signupLink = new RouterLink("Create account", SignupPage.class);
        signupLink.getStyle()
            .set("color", "#2a5885")
            .set("margin-left", "4px")
            .set("text-decoration", "none");
        
        signupLinkContainer.add(signupText, signupLink);
        
        // Add components to form container
        formContainer.add(logoContainer, emailFieldWrapper, passwordFieldWrapper, loginButton, signupLinkContainer);
        
        column.add(formContainer);
        return column;
    }
    
    // Validate form method
    private boolean validateForm() {
        boolean isValid = true;
        
        // Clear previous validation states
        emailField.setInvalid(false);
        passwordField.setInvalid(false);
        
        // Basic validation for empty fields
        if (emailField.getValue() == null || emailField.getValue().trim().isEmpty()) {
            emailField.setErrorMessage("Email is required");
            emailField.setInvalid(true);
            isValid = false;
        }
        
        if (passwordField.getValue() == null || passwordField.getValue().trim().isEmpty()) {
            passwordField.setErrorMessage("Password is required");
            passwordField.setInvalid(true);
            isValid = false;
        }
        
        return isValid;
    }
    
    private VerticalLayout createImageColumn() {
        VerticalLayout column = new VerticalLayout();
        column.addClassName("image-column");
        column.setJustifyContentMode(JustifyContentMode.CENTER);
        column.setHeight("100%");
        column.getStyle()
            .set("background-color", "#2a5885")
            .set("color", "white")
            .set("padding", "2em");
        
        // Pattern background
        Div patternOverlay = new Div();
        patternOverlay.setClassName("pattern-overlay");
        patternOverlay.setSizeFull();
        patternOverlay.getStyle()
            .set("background-image", "radial-gradient(rgba(255, 255, 255, 0.1) 1px, transparent 1px)")
            .set("background-size", "20px 20px")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("justify-content", "center")
            .set("align-items", "center")
            .set("text-align", "center");
        
        H2 welcomeText = new H2("Welcome back!");
        welcomeText.getStyle()
            .set("font-size", "32px")
            .set("font-weight", "700")
            .set("margin-bottom", "16px");
        
        Paragraph welcomeDescription = new Paragraph("Sign in to continue your conversations and catch up with your messages.");
        welcomeDescription.getStyle()
            .set("font-size", "18px")
            .set("max-width", "400px")
            .set("line-height", "1.6")
            .set("margin", "0 auto");
        
        patternOverlay.add(welcomeText, welcomeDescription);
        column.add(patternOverlay);
        
        return column;
    }
}