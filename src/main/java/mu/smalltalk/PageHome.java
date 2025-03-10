package mu.smalltalk;



import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Route("/")
public class PageHome extends VerticalLayout {
    public PageHome() {
        String img = """
                 <img src="data:image/png;base64, " alt="" />
                """;
        H1 header = new H1("Welcome to the SmallTalk");
        Html htmlimg = new Html(img);   
        add(header, htmlimg);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            
            try {
                InputStream inputStream = buffer.getInputStream();
                byte[] fileData = inputStream.readAllBytes();
                String encodedString = Base64.getEncoder().encodeToString(fileData);
                
                try {
                    Base64FileHandler.writeBase64ToFile(encodedString, fileName);
                    add(new Paragraph("Base64 content saved to file successfully!"));
                } catch (IOException e) {
                    add(new Paragraph("Error saving base64 content: " + e.getMessage()));
                }
                
                String img2 = "<img src='data:image/png;base64, " + encodedString + "' width='200px' />";
                add(new Html(img2), new H2(fileName), new Hr());
                
            } catch (IOException e) {
                add(new Paragraph("Error processing file: " + e.getMessage()));
            }
        });

        Button loadButton = new Button("Load Last Image", event -> {
            try {
                String lastFileName = "last_image"; 
                String encodedString = Base64FileHandler.readBase64FromFile(lastFileName);
                String img3 = "<img src='data:image/png;base64, " + encodedString + "' width='200px' />";
                add(new Html(img3), new H2("Loaded from file"), new Hr());
            } catch (IOException e) {
                add(new Paragraph("Error loading base64 content: " + e.getMessage()));
            }
        });

        add(upload, loadButton);
    }
}