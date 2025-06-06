package mu.smalltalk.Pages;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.beans.factory.annotation.Autowired;

import mu.smalltalk.Services.UserService;
import mu.smalltalk.entitis.User;


@CssImport("./styles/shared-styles.css")
@Route("/signup")
public class SignupPage extends VerticalLayout {

    private final UserService userService;

    @Autowired
    public SignupPage(UserService userService) {
        this.userService = userService;
        
        // Set basic page properties
        addClassName("signup-page");
        setSpacing(false);
        setMargin(false);
        setPadding(false);
        setSizeFull();
        getStyle().set("background-color", "#f5f7fa");
        
        // Add navigation bar (same as in HomePage)
        HorizontalLayout navbar = createNavigationBar();
        
        // Create the two-column layout
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setMargin(false);
        mainLayout.setSpacing(false);
        
        // Left column - Signup form
        VerticalLayout leftColumn = createSignupFormColumn();
        
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
        
        // Add logo and text to the container
        logoContainer.add(logo, logoText);
        
        // Add click listener to navigate to home page
        logoContainer.addClickListener(e -> logoContainer.getUI().ifPresent(ui -> ui.navigate("")));
        
        // Navigation links
        HorizontalLayout navLinks = new HorizontalLayout();
        navLinks.setSpacing(true);
        
        Anchor homeLink = new Anchor("", "Home");
        Anchor chatLink = new Anchor("chat", "Chat");
        Anchor loginLink = new Anchor("login", "Login");
        Anchor signupLink = new Anchor("signup", "Signup");
        signupLink.getStyle().set("font-weight", "bold"); // Bold to indicate current page
        Anchor featuresLink = new Anchor("#features", "Features");
        Anchor securityLink = new Anchor("#security", "Security");
        Anchor techLink = new Anchor("#technology", "Technology");
        Anchor aboutLink = new Anchor("#about", "About");
        
        navLinks.add(homeLink, featuresLink, securityLink, techLink, aboutLink, chatLink, loginLink,  signupLink);
        
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
        
        // Add logo container and navigation menu to the navbar
        navbar.add(logoContainer, navLinks);
        return navbar;
    }
    
    // Rest of your existing methods remain unchanged
    private VerticalLayout createSignupFormColumn() {
        // Existing code...
        /* Your existing createSignupFormColumn method remains unchanged */
        VerticalLayout column = new VerticalLayout();
        column.addClassName("signup-form-column");
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
        
        H2 title = new H2("Create Account");
        title.getStyle()
            .set("margin", "8px 0 0 0")
            .set("font-size", "24px")
            .set("font-weight", "700");
        
        Paragraph subtitle = new Paragraph("Get started with your free account");
        subtitle.getStyle()
            .set("margin", "0")
            .set("color", "rgba(0, 0, 0, 0.6)")
            .set("font-size", "16px");
        
        logoContainer.add(iconCircle, title, subtitle);
        
        // Full Name field
        Div fullNameFieldWrapper = new Div();
        fullNameFieldWrapper.setWidthFull();
        fullNameFieldWrapper.getStyle().set("margin-bottom", "16px");
        
        H5 fullNameLabel = new H5("Full Name");
        fullNameLabel.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-weight", "500");
        
        TextField fullNameField = new TextField();
        fullNameField.setWidthFull();
        fullNameField.setPlaceholder("John Doe");
        fullNameField.setPrefixComponent(VaadinIcon.USER.create());
        
        fullNameFieldWrapper.add(fullNameLabel, fullNameField);
        
        // Email field
        Div emailFieldWrapper = new Div();
        emailFieldWrapper.setWidthFull();
        emailFieldWrapper.getStyle().set("margin-bottom", "16px");
        
        H5 emailLabel = new H5("Email");
        emailLabel.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-weight", "500");
        
        EmailField emailField = new EmailField();
        emailField.setWidthFull();
        emailField.setPlaceholder("you@example.com");
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        
        emailFieldWrapper.add(emailLabel, emailField);
        
        // Password field
        Div passwordFieldWrapper = new Div();
        passwordFieldWrapper.setWidthFull();
        passwordFieldWrapper.getStyle().set("margin-bottom", "24px");
        
        H5 passwordLabel = new H5("Password");
        passwordLabel.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-weight", "500");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setWidthFull();
        passwordField.setPlaceholder("••••••••");
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        
        // Show password button
        Button showPasswordButton = new Button(new Icon(VaadinIcon.EYE));
        showPasswordButton.getStyle()
            .set("background", "none")
            .set("border", "none")
            .set("cursor", "pointer")
            .set("color", "rgba(0, 0, 0, 0.4)");
        
        showPasswordButton.addClickListener(e -> {
            if (passwordField.getElement().getProperty("type").equals("password")) {
                passwordField.getElement().setProperty("type", "text");
                showPasswordButton.setIcon(new Icon(VaadinIcon.EYE_SLASH));
            } else {
                passwordField.getElement().setProperty("type", "password");
                showPasswordButton.setIcon(new Icon(VaadinIcon.EYE));
            }
        });
        
        passwordField.setSuffixComponent(showPasswordButton);
        passwordFieldWrapper.add(passwordLabel, passwordField);
        
        // Signup button
        Button signupButton = new Button("Create Account");
        signupButton.getStyle()
            .set("background-color", "#2a5885")
            .set("color", "white")
            .set("border-radius", "4px")
            .set("font-weight", "500")
            .set("cursor", "pointer");
        signupButton.setWidthFull();
        signupButton.setHeight("48px");
        
        // Add improved signup button click handler with better debugging
        signupButton.addClickListener(event -> {
            try {
                String fullName = fullNameField.getValue();
                String email = emailField.getValue();
                String password = passwordField.getValue();
                
                // Debug logging - step 1
                System.out.println("Attempting registration with values: Name=" + fullName + ", Email=" + email);
                
                // Validate inputs
                if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Notification.show("Please fill in all fields", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                // Debug logging - step 2
                System.out.println("Input validation passed, calling userService.registerUser()");
                
                // Call the service to register the user
                User registeredUser = userService.registerUser(fullName, email, password);
                
                // Debug logging - step 3
                System.out.println("User registered successfully: " + registeredUser);
                
                // Show success notification
                Notification.show("Account created successfully!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                // Redirect to login page
                UI.getCurrent().navigate("/login");
            } catch (Exception e) {
                // Enhanced error logging
                System.err.println("Registration failed with exception: " + e.getClass().getName());
                System.err.println("Error message: " + e.getMessage());
                e.printStackTrace();
                
                // Show error notification
                Notification.show("Registration failed: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        // Login link
        Div loginLinkContainer = new Div();
        loginLinkContainer.getStyle()
            .set("display", "flex")
            .set("justify-content", "center")
            .set("margin-top", "24px")
            .set("width", "100%");
        
        Span loginText = new Span("Already have an account? ");
        loginText.getStyle().set("color", "rgba(0, 0, 0, 0.6)");
        
        RouterLink loginLink = new RouterLink("Sign in", LoginPage.class);
        loginLink.getStyle()
            .set("color", "#2a5885")
            .set("margin-left", "4px")
            .set("text-decoration", "none");
        
        loginLinkContainer.add(loginText, loginLink);
        
        // Add components to form container
        formContainer.add(logoContainer, fullNameFieldWrapper, emailFieldWrapper, passwordFieldWrapper, signupButton, loginLinkContainer);
        
        column.add(formContainer);
        return column;
    }
    
    private VerticalLayout createImageColumn() {
        // Existing code...
        /* Your existing createImageColumn method remains unchanged */
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
        
        H2 welcomeText = new H2("Join our community");
        welcomeText.getStyle()
            .set("font-size", "32px")
            .set("font-weight", "700")
            .set("margin-bottom", "16px");
        
        Paragraph welcomeDescription = new Paragraph("Connect with friends, share moments, and stay in touch with your loved ones.");
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