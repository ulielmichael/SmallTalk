package mu.smalltalk.secriuty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Base64FileHandler {
    public static void writeBase64ToFile(String base64Content, String fileName) throws IOException {
        Files.write(Paths.get(fileName), base64Content.getBytes());
    }

    public static String readBase64FromFile(String fileName) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(fileName));
        return new String(fileBytes);
    }
}
