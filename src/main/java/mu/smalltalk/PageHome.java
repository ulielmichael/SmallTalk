package mu.smalltalk;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@CssImport("./styles/shared-styles.css")
@Route("/")
public class PageHome extends VerticalLayout {
    
    public PageHome() {
        // Set basic page properties
        addClassName("home-page");
        setSpacing(false);
        setMargin(false);
        setPadding(true);
        setSizeFull();
        getStyle().set("background-color", "#f5f7fa");
        
        // Navigation bar
        HorizontalLayout navbar = createNavigationBar();
        
        // Hero section
        Div heroSection = createHeroSection();
        
        // Features section
        Div featuresSection = createFeaturesSection();
        Div ChatSection = createFeaturesSection();
        
        // Security section
        Div securitySection = createSecuritySection();
        
        // Technology section
        Div technologySection = createTechnologySection();

        // Div LoginSection = createloginSection();
        
        // Footer
        Div footer = createFooter();
        
        // Add all components to the main layout
        add(navbar, heroSection, featuresSection, securitySection, technologySection, footer);
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
        
        // Logo
        H2 logo = new H2("SmallTalk");
        logo.getStyle()
            .set("margin", "0")
            .set("color", "#2a5885")
            .set("font-weight", "700");
        
        // Navigation links
        HorizontalLayout navLinks = new HorizontalLayout();
        navLinks.setSpacing(true);
        
        Anchor homeLink = new Anchor("#", "Home");
        homeLink.getStyle().set("font-weight", "bold");

        Anchor chatlink = new Anchor("chat", "Chat");
        Anchor loginLink = new Anchor("login", "Login");
        Anchor logoutLink = new Anchor("logout", "Logout");
        Anchor signupLink = new Anchor("signup", "Signup");
        Anchor featuresLink = new Anchor("#features", "Features");
        Anchor securityLink = new Anchor("#security", "Security");
        Anchor techLink = new Anchor("#technology", "Technology");
        Anchor aboutLink = new Anchor("#about", "About");
        
        navLinks.add(homeLink, featuresLink, securityLink, techLink, aboutLink, chatlink, loginLink, logoutLink, signupLink);
        
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
        
        navbar.add(logo, navLinks);
        return navbar;
    }
    
    private Div createHeroSection() {
        Div heroSection = new Div();
        heroSection.setWidthFull();
        heroSection.addClassName("hero-section");
        heroSection.getStyle()
            .set("background-color", "#2a5885")
            .set("color", "white")
            .set("padding", "80px 24px")
            .set("text-align", "center");
        
        H1 title = new H1("SmallTalk");
        title.getStyle()
            .set("font-size", "48px")
            .set("margin-bottom", "16px");
        
        H3 subtitle = new H3("Secure End-to-End Encrypted Communication Platform");
        subtitle.getStyle()
            .set("font-weight", "400")
            .set("margin-top", "0")
            .set("margin-bottom", "32px");
        
        Paragraph description = new Paragraph("SmallTalk provides a comprehensive solution for the growing need for privacy and data security through an End-to-End encryption mechanism. Designed for private and group conversations, encrypted file sharing in real-time, ensuring complete protection of user data.");
        description.getStyle()
            .set("max-width", "800px")
            .set("margin", "0 auto 32px")
            .set("font-size", "18px")
            .set("line-height", "1.6");
        
        // Action button
        Anchor ctaButton = new Anchor("#features", "Learn More");
        ctaButton.getStyle()
            .set("background-color", "white")
            .set("color", "#2a5885")
            .set("padding", "12px 32px")
            .set("border-radius", "4px")
            .set("font-weight", "bold")
            .set("text-decoration", "none")
            .set("display", "inline-block")
            .set("transition", "background-color 0.3s")
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        
        heroSection.add(title, subtitle, description, ctaButton);
        return heroSection;
    }
    
    private Div createFeaturesSection() {
        Div featuresSection = new Div();
        featuresSection.setId("features");
        featuresSection.setWidthFull();
        featuresSection.getStyle()
            .set("padding", "80px 24px")
            .set("background-color", "white");
        
        H2 sectionTitle = new H2("Key Features");
        sectionTitle.getStyle()
            .set("text-align", "center")
            .set("margin-bottom", "48px")
            .set("color", "#2a5885");
        
        HorizontalLayout featuresContainer = new HorizontalLayout();
        featuresContainer.setWidthFull();
        featuresContainer.setJustifyContentMode(JustifyContentMode.CENTER);
        featuresContainer.setSpacing(true);
        featuresContainer.setPadding(true);
        
        Div feature1 = createFeatureBox("Private Messaging", "Secure private and group conversations with full end-to-end encryption.");
        Div feature2 = createFeatureBox("Encrypted File Sharing", "Share files with real-time encryption, ensuring data protection at all times.");
        Div feature3 = createFeatureBox("Identity Verification", "Advanced authentication mechanisms to verify sender and recipient identities.");
        Div feature4 = createFeatureBox("Real-time Sync", "Messages and notifications update in real-time using WebSocket technology.");
        
        featuresContainer.add(feature1, feature2, feature3, feature4);
        featuresSection.add(sectionTitle, featuresContainer);
        
        return featuresSection;
    }
    
    private Div createSecuritySection() {
        Div securitySection = new Div();
        securitySection.setId("security");
        securitySection.setWidthFull();
        securitySection.getStyle()
            .set("padding", "80px 24px")
            .set("background-color", "#f5f7fa");
        
        H2 sectionTitle = new H2("Advanced Security");
        sectionTitle.getStyle()
            .set("text-align", "center")
            .set("margin-bottom", "48px")
            .set("color", "#2a5885");
        
        VerticalLayout securityContent = new VerticalLayout();
        securityContent.setMaxWidth("800px");
        securityContent.setAlignItems(Alignment.CENTER);
        securityContent.getStyle().set("margin", "0 auto");
        
        // Security algorithm details
        Div algorithm = new Div();
        algorithm.getStyle()
            .set("background-color", "white")
            .set("border-radius", "8px")
            .set("padding", "24px")
            .set("margin-bottom", "32px")
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.05)")
            .set("width", "100%");
        
        H3 algoTitle = new H3("State-of-the-art Cryptography");
        
        Div algoDetails = new Div();
        
        H4 ecdhTitle = new H4("ECDH Key Exchange");
        Paragraph ecdhDesc = new Paragraph("Elliptic Curve Diffie-Hellman protocol for secure key exchange between users without requiring prior contact or shared secrets.");
        
        H4 aesTitle = new H4("AES-256 Encryption");
        Paragraph aesDesc = new Paragraph("Advanced Encryption Standard with 256-bit key size for message encryption, providing one of the strongest encryption levels available today.");
        
        H4 sigTitle = new H4("Digital Signatures");
        Paragraph sigDesc = new Paragraph("Cryptographic mechanism to verify message integrity and sender authenticity, preventing tampering and ensuring non-repudiation.");
        
        algoDetails.add(ecdhTitle, ecdhDesc, aesTitle, aesDesc, sigTitle, sigDesc);
        algorithm.add(algoTitle, algoDetails);
        
        // Security benefits
        H3 benefitsTitle = new H3("Security Benefits");
        benefitsTitle.getStyle().set("margin-top", "16px");
        
        Div benefits = new Div();
        benefits.getStyle()
            .set("background-color", "white")
            .set("border-radius", "8px")
            .set("padding", "24px")
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.05)")
            .set("width", "100%");
        
        Paragraph benefit1 = new Paragraph("• Complete message confidentiality - preventing access to message content from any external source, including the server itself");
        Paragraph benefit2 = new Paragraph("• Secure key exchange - creating a communication channel where shared keys are generated dynamically and securely");
        Paragraph benefit3 = new Paragraph("• Identity authentication - mechanism that verifies sender and recipient identities without relying on third parties");
        Paragraph benefit4 = new Paragraph("• Real-time performance and synchronization - combining advanced security with high response speed");
        
        benefits.add(benefit1, benefit2, benefit3, benefit4);
        
        securityContent.add(algorithm, benefitsTitle, benefits);
        securitySection.add(sectionTitle, securityContent);
        
        return securitySection;
    }
    
    private Div createTechnologySection() {
        Div techSection = new Div();
        techSection.setId("technology");
        techSection.setWidthFull();
        techSection.getStyle()
            .set("padding", "80px 24px")
            .set("background-color", "white");
        
        H2 sectionTitle = new H2("Technology Stack");
        sectionTitle.getStyle()
            .set("text-align", "center")
            .set("margin-bottom", "48px")
            .set("color", "#2a5885");
        
        VerticalLayout techContent = new VerticalLayout();
        techContent.setMaxWidth("800px");
        techContent.setAlignItems(Alignment.CENTER);
        techContent.getStyle().set("margin", "0 auto");
        
        // Three-tier architecture description
        Div architecture = new Div();
        architecture.getStyle()
            .set("background-color", "#f5f7fa")
            .set("border-radius", "8px")
            .set("padding", "24px")
            .set("margin-bottom", "32px")
            .set("width", "100%");
        
        H3 archTitle = new H3("Three-Tier Architecture");
        
        H4 tier1Title = new H4("Presentation Layer");
        Paragraph tier1Desc = new Paragraph("Built with Vaadin framework, providing a secure and responsive user interface with client-side encryption capabilities.");
        
        H4 tier2Title = new H4("Business Logic Layer");
        Paragraph tier2Desc = new Paragraph("Powered by Spring Boot and TomCat server, handling core business logic, REST API, WebSocket communication, and security protocols.");
        
        H4 tier3Title = new H4("Data Layer");
        Paragraph tier3Desc = new Paragraph("MongoDB NoSQL database for flexible document storage, high performance, and native encryption support.");
        
        architecture.add(archTitle, tier1Title, tier1Desc, tier2Title, tier2Desc, tier3Title, tier3Desc);
        
        // Communication protocols
        Div protocols = new Div();
        protocols.getStyle()
            .set("background-color", "#f5f7fa")
            .set("border-radius", "8px")
            .set("padding", "24px")
            .set("width", "100%");
        
        H3 protocolTitle = new H3("Communication Protocols");
        
        Paragraph protocol1 = new Paragraph("• WebSocket for real-time bi-directional communication");
        Paragraph protocol2 = new Paragraph("• HTTP/HTTPS for client-server communication");
        Paragraph protocol3 = new Paragraph("• TCP for reliable data transfer");
        Paragraph protocol4 = new Paragraph("• SSL/TLS for secure data transmission");
        
        protocols.add(protocolTitle, protocol1, protocol2, protocol3, protocol4);
        
        techContent.add(architecture, protocols);
        techSection.add(sectionTitle, techContent);
        
        return techSection;
    }
    
    private Div createFooter() {
        Div footer = new Div();
        footer.setId("about");
        footer.setWidthFull();
        footer.getStyle()
            .set("background-color", "#2a5885")
            .set("color", "white")
            .set("padding", "40px 24px")
            .set("text-align", "center");
        
        H3 projectInfo = new H3("SmallTalk Project");
        projectInfo.getStyle().set("margin-bottom", "16px");
        
        Paragraph projectDesc = new Paragraph("A secure end-to-end encrypted communication platform developed as a final project for Software Engineering.");
        projectDesc.getStyle()
            .set("max-width", "600px")
            .set("margin", "0 auto 24px");
        
        Paragraph credits = new Paragraph("Developed by Uliel Michael  • 2025");
        
        footer.add(projectInfo, projectDesc, credits);
        
        return footer;
    }
    
    private Div createFeatureBox(String title, String content) {
        Div box = new Div();
        box.getStyle()
            .set("background-color", "#f5f7fa")
            .set("padding", "24px")
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.05)")
            .set("width", "220px")
            .set("height", "220px")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("text-align", "center")
            .set("transition", "transform 0.3s, box-shadow 0.3s");
        
        H3 boxTitle = new H3(title);
        boxTitle.getStyle()
            .set("margin-top", "0")
            .set("margin-bottom", "16px")
            .set("color", "#2a5885");
        
        Paragraph boxContent = new Paragraph(content);
        boxContent.getStyle()
            .set("margin", "0")
            .set("line-height", "1.5");
        
        box.add(boxTitle, boxContent);
        return box;
    }
}