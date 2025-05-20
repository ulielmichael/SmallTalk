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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.beans.factory.annotation.Autowired;

import mu.smalltalk.Services.UserService;

@CssImport("./styles/shared-styles.css")
@Route("/logout")
public class LogoutPage extends VerticalLayout {

    private final UserService userService;

    @Autowired
    public LogoutPage(UserService userService) {
        this.userService = userService;
        
        // Set basic page properties
        addClassName("logout-page");
        setSpacing(false);
        setMargin(false);
        setPadding(false);
        setSizeFull();
        getStyle().set("background-color", "#f5f7fa");
        
        // Add navigation bar
        HorizontalLayout navbar = createNavigationBar();
        
        // Create the two-column layout
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setMargin(false);
        mainLayout.setSpacing(false);
        
        // Left column - Logout confirmation
        VerticalLayout leftColumn = createLogoutColumn();
        
        // Right column - Image/Pattern
        VerticalLayout rightColumn = createImageColumn();
        
        // Add columns to main layout
        mainLayout.add(leftColumn, rightColumn);
        mainLayout.setFlexGrow(1, leftColumn);
        mainLayout.setFlexGrow(1, rightColumn);
        
        add(navbar, mainLayout);
    }
    
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
        
        // Logo container with text - horizontal layout for logo and text
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
        
        // Add click listener to navigate to home page
        logoContainer.addClickListener(e -> logoContainer.getUI().ifPresent(ui -> ui.navigate("")));
        
        // Navigation links
        HorizontalLayout navLinks = new HorizontalLayout();
        navLinks.setSpacing(true);
        
        Anchor homeLink = new Anchor("", "Home");
        Anchor chatLink = new Anchor("chat", "Chat");
        Anchor loginLink = new Anchor("login", "Login");
        Anchor logoutLink = new Anchor("logout", "Logout");
        logoutLink.getStyle().set("font-weight", "bold"); // Highlight the current page
        Anchor signupLink = new Anchor("signup", "Signup");
        Anchor featuresLink = new Anchor("#features", "Features");
        Anchor securityLink = new Anchor("#security", "Security");
        Anchor techLink = new Anchor("#technology", "Technology");
        Anchor aboutLink = new Anchor("#about", "About");
        
        navLinks.add(homeLink, featuresLink, securityLink, techLink, aboutLink, chatLink, loginLink, logoutLink, signupLink);
        
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
        
        // Add logo container and navigation links to navbar
        navbar.add(logoContainer, navLinks);
        return navbar;
    }
    
    private VerticalLayout createLogoutColumn() {
        VerticalLayout column = new VerticalLayout();
        column.addClassName("logout-form-column");
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
            
        Icon logoutIcon = VaadinIcon.SIGN_OUT.create();
        logoutIcon.setSize("24px");
        logoutIcon.setColor("#2a5885");
        iconCircle.add(logoutIcon);
        
        H2 title = new H2("Sign Out");
        title.getStyle()
            .set("margin", "8px 0 0 0")
            .set("font-size", "24px")
            .set("font-weight", "700");
        
        Paragraph subtitle = new Paragraph("Are you sure you want to sign out?");
        subtitle.getStyle()
            .set("margin", "0")
            .set("color", "rgba(0, 0, 0, 0.6)")
            .set("font-size", "16px");
        
        logoContainer.add(iconCircle, title, subtitle);
        
        // Logout confirmation message
        Div messageContainer = new Div();
        messageContainer.getStyle()
            .set("text-align", "center")
            .set("margin-bottom", "32px");
        
        Paragraph message = new Paragraph("You will be logged out of your account and redirected to the login page.");
        message.getStyle()
            .set("color", "rgba(0, 0, 0, 0.6)")
            .set("font-size", "16px")
            .set("line-height", "1.6");
        
        messageContainer.add(message);
        
        // Button container for horizontal layout
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(false);
        
        // Cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.getStyle()
            .set("background-color", "#f5f5f5")
            .set("color", "#333")
            .set("border", "1px solid #ddd")
            .set("border-radius", "4px")
            .set("font-weight", "500")
            .set("cursor", "pointer");
        cancelButton.setHeight("48px");
        cancelButton.setMinWidth("140px");
        
        // Cancel button click handler
        cancelButton.addClickListener(e -> {
            // Navigate back to chat or home page
            UI.getCurrent().navigate("/");
        });
        
        // Logout button
        Button logoutButton = new Button("Sign Out");
        logoutButton.getStyle()
            .set("background-color", "#2a5885")
            .set("color", "white")
            .set("border-radius", "4px")
            .set("font-weight", "500")
            .set("cursor", "pointer");
        logoutButton.setHeight("48px");
        logoutButton.setMinWidth("140px");
        
        // Add logout button click handler
        logoutButton.addClickListener(e -> {
            try {
                // Call the logout method from userService
                userService.logout();
                
                // Show success notification
                Notification.show("You have been signed out successfully", 
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                // Redirect to login page
                UI.getCurrent().navigate("/login");
                
            } catch (Exception ex) {
                // Show error message
                Notification.show("Logout failed: " + ex.getMessage(), 
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        buttonLayout.add(cancelButton, logoutButton);
        
        // Login link
        Div loginLinkContainer = new Div();
        loginLinkContainer.getStyle()
            .set("display", "flex")
            .set("justify-content", "center")
            .set("margin-top", "24px")
            .set("width", "100%");
        
        Span loginText = new Span("Return to ");
        loginText.getStyle().set("color", "rgba(0, 0, 0, 0.6)");
        
        RouterLink loginLink = new RouterLink("Sign in", LoginPage.class);
        loginLink.getStyle()
            .set("color", "#2a5885")
            .set("margin-left", "4px")
            .set("text-decoration", "none");
        
        loginLinkContainer.add(loginText, loginLink);
        
        // Add components to form container
        formContainer.add(logoContainer, messageContainer, buttonLayout, loginLinkContainer);
        
        column.add(formContainer);
        return column;
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
        
        H2 goodbyeText = new H2("See you again soon!");
        goodbyeText.getStyle()
            .set("font-size", "32px")
            .set("font-weight", "700")
            .set("margin-bottom", "16px");
        
        Paragraph goodbyeDescription = new Paragraph("Thank you for using our platform. We look forward to your next visit.");
        goodbyeDescription.getStyle()
            .set("font-size", "18px")
            .set("max-width", "400px")
            .set("line-height", "1.6")
            .set("margin", "0 auto");
        
        patternOverlay.add(goodbyeText, goodbyeDescription);
        column.add(patternOverlay);
        
        return column;
    }
}