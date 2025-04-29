package mu.smalltalk;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import mu.smalltalk.entitis.User;
import mu.smalltalk.Services.UserService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Route("profile")
@PageTitle("User Profile")
public class ProfilPage extends VerticalLayout {
    
    private final UserService userService;
    private User currentUser;
    private Image profileImage;
    
    public ProfilPage(UserService userService) {
        this.userService = userService;
        this.currentUser = userService.getCurrentUser();
        
        addClassName("profile-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        
        // Set LTR direction for English support
        getElement().setAttribute("dir", "ltr");
        
        Div contentContainer = new Div();
        contentContainer.addClassName("content-container");
        contentContainer.getStyle()
            .set("max-width", "32rem")
            .set("margin", "auto")
            .set("padding", "2rem")
            .set("background-color", "var(--lumo-base-color)")
            .set("border-radius", "1rem")
            .set("box-shadow", "var(--lumo-box-shadow-s)");
        
        // Header section
        H1 title = new H1("Profile");
        title.getStyle().set("margin-bottom", "0");
        
        Paragraph subtitle = new Paragraph("Your user details");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        VerticalLayout headerSection = new VerticalLayout(title, subtitle);
        headerSection.setAlignItems(FlexComponent.Alignment.CENTER);
        headerSection.setPadding(false);
        headerSection.setSpacing(false);
        
        contentContainer.add(headerSection);
        
        // Profile image section
        createProfileImageSection(contentContainer);
        
        // User details section
        createUserDetailsSection(contentContainer);
        
        // Account information section
        createAccountInfoSection(contentContainer);
        
        add(contentContainer);
    }
    
    private void createProfileImageSection(Div container) {
        VerticalLayout imageSection = new VerticalLayout();
        imageSection.setAlignItems(FlexComponent.Alignment.CENTER);
        
        Div imageContainer = new Div();
        imageContainer.getStyle()
            .set("position", "relative")
            .set("width", "8rem")
            .set("height", "8rem");
        
        profileImage = new Image();
        
        // Use the User class's getProfilePic method
        String profileImageData = currentUser != null ? currentUser.getProfilePic() : null;
        
        if (profileImageData != null) {
            profileImage.setSrc(createImageResource(profileImageData));
        } else {
            profileImage.setSrc("images/avatar.png");
        }
        
        profileImage.setAlt("Profile Picture");
        profileImage.getStyle()
            .set("width", "8rem")
            .set("height", "8rem")
            .set("border-radius", "50%")
            .set("object-fit", "cover")
            .set("border", "4px solid var(--lumo-contrast-10pct)");
        
        // Upload button
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.setAcceptedFileTypes("image/*");
        
        Button uploadButton = new Button(VaadinIcon.CAMERA.create());
        uploadButton.getStyle()
            .set("position", "absolute")
            .set("bottom", "0")
            // Position on right side in LTR layout
            .set("right", "0")
            .set("background-color", "var(--lumo-contrast)")
            .set("color", "var(--lumo-base-color)")
            .set("padding", "0.5rem")
            .set("border-radius", "50%")
            .set("cursor", "pointer")
            // Make the button more prominent
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.2)")
            .set("z-index", "10");
        
        upload.setUploadButton(uploadButton);
        // Hide the upload component's default elements
        upload.getStyle()
            .set("position", "absolute")
            .set("bottom", "0") 
            .set("right", "0")
            .set("margin", "0")
            .set("padding", "0");
        
        // Hide the dropzone and file list components
        upload.setDropAllowed(false);
        upload.setDropLabel(new Span(""));
        
        upload.addSucceededListener(event -> {
            try {
                byte[] imageBytes = buffer.getInputStream().readAllBytes();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                
                // Update user profile pic
                if (currentUser != null) {
                    currentUser.setProfilePic(base64Image);
                    userService.updateProfilePicture(currentUser);
                    
                    // Update the image in UI
                    profileImage.setSrc(createImageResource(base64Image));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
        Paragraph uploadHint = new Paragraph("Click the camera icon to update your picture");
        uploadHint.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)")
            .set("margin-top", "0.75rem")
            .set("margin-bottom", "0");
        
        imageContainer.add(profileImage, upload);
        imageSection.add(imageContainer, uploadHint);
        container.add(imageSection);
    }
    
    private void createUserDetailsSection(Div container) {
        VerticalLayout detailsSection = new VerticalLayout();
        detailsSection.setPadding(false);
        detailsSection.setSpacing(true);
        
        // Full Name field
        VerticalLayout nameLayout = new VerticalLayout();
        nameLayout.setPadding(false);
        nameLayout.setSpacing(false);
        
        HorizontalLayout nameLabel = new HorizontalLayout();
        nameLabel.setAlignItems(FlexComponent.Alignment.CENTER);
        nameLabel.setSpacing(true);
        nameLabel.add(VaadinIcon.USER.create(), new Span("Full Name"));
        nameLabel.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)");
        
        Div nameValue = new Div();
        nameValue.setText(currentUser != null ? currentUser.getFullName() : "");
        nameValue.getStyle()
            .set("padding", "0.75rem 1rem")
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border-radius", "0.5rem")
            .set("border", "1px solid var(--lumo-contrast-10pct)");
        
        nameLayout.add(nameLabel, nameValue);
        
        // Email field
        VerticalLayout emailLayout = new VerticalLayout();
        emailLayout.setPadding(false);
        emailLayout.setSpacing(false);
        
        HorizontalLayout emailLabel = new HorizontalLayout();
        emailLabel.setAlignItems(FlexComponent.Alignment.CENTER);
        emailLabel.setSpacing(true);
        emailLabel.add(VaadinIcon.ENVELOPE.create(), new Span("Email Address"));
        emailLabel.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)");
        
        Div emailValue = new Div();
        emailValue.setText(currentUser != null ? currentUser.getEmail() : "");
        emailValue.getStyle()
            .set("padding", "0.75rem 1rem")
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border-radius", "0.5rem")
            .set("border", "1px solid var(--lumo-contrast-10pct)");
        
        emailLayout.add(emailLabel, emailValue);
        
        detailsSection.add(nameLayout, emailLayout);
        container.add(detailsSection);
    }
    
    private void createAccountInfoSection(Div container) {
        Div accountSection = new Div();
        accountSection.getStyle()
            .set("margin-top", "1.5rem")
            .set("padding", "1.5rem")
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border-radius", "0.75rem");
        
        H2 accountTitle = new H2("Account Information");
        accountTitle.getStyle()
            .set("margin-top", "0")
            .set("font-size", "1.125rem")
            .set("margin-bottom", "1rem");
        
        VerticalLayout accountDetails = new VerticalLayout();
        accountDetails.setPadding(false);
        accountDetails.setSpacing(false);
        
        // Member since row
        HorizontalLayout memberSinceRow = new HorizontalLayout();
        memberSinceRow.setWidthFull();
        memberSinceRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        memberSinceRow.getStyle()
            .set("padding", "0.5rem 0")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        
        Span memberSinceLabel = new Span("Member Since");
        
        String joinDate = currentUser != null && currentUser.getCreatedAt() != null ? 
            currentUser.getCreatedAt().format(DateTimeFormatter.ISO_DATE) : "Not Available";
        Span memberSinceValue = new Span(joinDate);
        
        memberSinceRow.add(memberSinceLabel, memberSinceValue);
        
        // Last login row
        HorizontalLayout lastLoginRow = new HorizontalLayout();
        lastLoginRow.setWidthFull();
        lastLoginRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        lastLoginRow.getStyle()
            .set("padding", "0.5rem 0")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        
        Span lastLoginLabel = new Span("Last Login");
        
        String lastLoginDate = currentUser != null && currentUser.getLastLogin() != null ?
            currentUser.getLastLogin().format(DateTimeFormatter.ISO_DATE) : "Never";
        Span lastLoginValue = new Span(lastLoginDate);
        
        lastLoginRow.add(lastLoginLabel, lastLoginValue);
        
        // Account status row
        HorizontalLayout statusRow = new HorizontalLayout();
        statusRow.setWidthFull();
        statusRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        statusRow.getStyle().set("padding", "0.5rem 0");
        
        Span statusLabel = new Span("Account Status");
        Span statusValue = new Span("Active");
        statusValue.getStyle().set("color", "var(--lumo-success-color)");
        
        statusRow.add(statusLabel, statusValue);
        
        accountDetails.add(memberSinceRow, lastLoginRow, statusRow);
        accountSection.add(accountTitle, accountDetails);
        container.add(accountSection);
    }
    
    private StreamResource createImageResource(String base64Image) {
        String base64Data = base64Image;
        
        // Remove data URL prefix if present
        if (base64Data.contains(",")) {
            base64Data = base64Data.split(",")[1];
        }
        
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        
        return new StreamResource("profile-image.png", 
            () -> new ByteArrayInputStream(imageBytes));
    }
}