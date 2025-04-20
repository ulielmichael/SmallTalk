package mu.smalltalk;



import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * A reusable component for creating the patterned background image section
 * used in authentication pages like login and signup.
 */
public class AuthImagePattern extends VerticalLayout {
    
    /**
     * Constructor for the auth image pattern component
     * 
     * @param title The title text to display
     * @param subtitle The subtitle text to display
     */
    public AuthImagePattern(String title, String subtitle) {
        addClassName("auth-image-pattern");
        setJustifyContentMode(JustifyContentMode.CENTER);
        setHeight("100%");
        getStyle()
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
        
        H2 titleElement = new H2(title);
        titleElement.getStyle()
            .set("font-size", "32px")
            .set("font-weight", "700")
            .set("margin-bottom", "16px");
        
        Paragraph subtitleElement = new Paragraph(subtitle);
        subtitleElement.getStyle()
            .set("font-size", "18px")
            .set("max-width", "400px")
            .set("line-height", "1.6")
            .set("margin", "0 auto");
        
        patternOverlay.add(titleElement, subtitleElement);
        add(patternOverlay);
    }
}