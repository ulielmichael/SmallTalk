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
        // כותרת עליונה עם ניווט
        HorizontalLayout navbar = new HorizontalLayout();
        navbar.getStyle().set("background-color", "#f8f9fa");
        navbar.getStyle().set("padding", "10px");
        navbar.getStyle().set("border-bottom", "1px solid #ddd");

        Anchor homeLink = new Anchor("#", "Home");
        Anchor aboutLink = new Anchor("#", "About");
        Anchor advantagesLink = new Anchor("#", "Advantages");

        navbar.add(homeLink, aboutLink, advantagesLink);

        // כותרת מרכזית
        H1 title = new H1("The Basics of Central Bank Digital Currency");
        title.getStyle().set("color", "white");
        title.getStyle().set("font-size", "36px");
        title.getStyle().set("text-align", "center");

        // גרפיקה מרכזית
        Image graphic = new Image("https://example.com/graphic.png", "Digital Currency Graphic");
        graphic.setWidth("80%");
        graphic.getStyle().set("margin", "auto");

        HorizontalLayout infoBoxes = new HorizontalLayout();
        infoBoxes.getStyle().set("margin-top", "20px");

        Div aboutBox = createInfoBox("About", "CBDC is a digital form of fiat currency issued and regulated by central banks.");
        Div storingBox = createInfoBox("Storing", "CBDC can be stored in digital wallets and accessed using a mobile phone or other electronic device.");
        Div functionsBox = createInfoBox("Functions", "CBDC functions like physical cash in digital form, enabling everyday transactions.");

        infoBoxes.add(aboutBox, storingBox, functionsBox);

        add(navbar, title, graphic, infoBoxes);
    }

    private Div createInfoBox(String title, String content) {
        Div box = new Div();
        box.getStyle().set("background-color", "#ffffff");
        box.getStyle().set("padding", "15px");
        box.getStyle().set("border-radius", "10px");
        box.getStyle().set("box-shadow", "0 4px 6px rgba(0, 0, 0, 0.1)");
        box.getStyle().set("width", "30%");

        H3 boxTitle = new H3(title);
        Paragraph boxContent = new Paragraph(content);

        box.add(boxTitle, boxContent);
        return box;
    }
}


