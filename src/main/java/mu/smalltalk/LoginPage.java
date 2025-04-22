package mu.smalltalk;

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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import mu.smalltalk.Services.UserService;

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
        
        add(mainLayout);
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
                    return;
                }
                
                // Try to authenticate the user
                User authenticatedUser = userService.authenticateUser(
                    emailField.getValue(), 
                    passwordField.getValue()
                );
                
                // If successful, navigate to chat page
                Notification.show("Login successful! Welcome, " + authenticatedUser.getFullName(), 
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                getUI().ifPresent(ui -> ui.navigate(Chat.class));
                
            } catch (Exception ex) {
                // Show error message
                Notification.show("Login failed: " + ex.getMessage(), 
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
        
        RouterLink signupLink = new RouterLink("Create account", PageSignup.class);
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
    
    private boolean validateForm() {
        boolean isValid = true;
        
        if (emailField.isEmpty() || !emailField.isInvalid()) {
            emailField.setInvalid(true);
            isValid = false;
        }
        
        if (passwordField.isEmpty()) {
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