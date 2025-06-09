package mu.smalltalk.Services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

@Service
public class QuoteService {
    
    private final RestTemplate restTemplate;
    
    public QuoteService() {
        this.restTemplate = createRestTemplate();
    }
    
    private RestTemplate createRestTemplate() {
        try {
            // יצירת TrustManager שמקבל כל תעודה
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            // התקנת ה-TrustManager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            // יצירת HostnameVerifier שמקבל כל hostname
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            
        } catch (Exception e) {
            // System.err.println("Error setting up SSL context: " + e.getMessage());
        }
        
        return new RestTemplate();
    }
    
    public Quote getRandomMotivationalQuote() {
        // ננסה קודם את ה-API הפשוט יותר
        try {
            String url = "https://zenquotes.io/api/random";
            ZenQuoteResponse[] response = restTemplate.getForObject(url, ZenQuoteResponse[].class);
            
            if (response != null && response.length > 0 && response[0].getQ() != null && response[0].getA() != null) {
                return new Quote(response[0].getQ(), response[0].getA());
            }
        } catch (RestClientException e) {
            // System.err.println("Error fetching quote from ZenQuotes API: " + e.getMessage());
        }
        
     
        
        // משפט ברירת מחדל במקרה של שגיאה
        return getDefaultQuote();
    }
    
    private Quote getDefaultQuote() {
        // מערך של משפטי חיזוק ברירת מחדל
        String[] defaultQuotes = {
            "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            "The only way to do great work is to love what you do.",
            "Innovation distinguishes between a leader and a follower.",
            "Your limitation—it's only your imagination.",
            "Push yourself, because no one else is going to do it for you.",
            "Great things never come from comfort zones.",
            "Dream it. Wish it. Do it.",
            "Success doesn't just find you. You have to go out and get it.",
            "The harder you work for something, the greater you'll feel when you achieve it.",
            "Don't stop when you're tired. Stop when you're done."
        };
        
        String[] authors = {
            "Winston Churchill",
            "Steve Jobs", 
            "Steve Jobs",
            "Anonymous",
            "Anonymous",
            "Anonymous",
            "Anonymous",
            "Anonymous",
            "Anonymous",
            "Anonymous"
        };
        
        int randomIndex = (int) (Math.random() * defaultQuotes.length);
        return new Quote(defaultQuotes[randomIndex], authors[randomIndex]);
    }
    
    // קלאס עזר לקבלת התגובה מ-Quotable API
    public static class QuotableResponse {
        private String content;
        private String author;
        private String[] tags;
        private int length;
        
        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String[] getTags() { return tags; }
        public void setTags(String[] tags) { this.tags = tags; }
        
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
    }
    
    // קלאס עזר לקבלת התגובה מ-ZenQuotes API
    public static class ZenQuoteResponse {
        private String q; // quote text
        private String a; // author
        private String h; // HTML formatted quote
        
        // Getters and Setters
        public String getQ() { return q; }
        public void setQ(String q) { this.q = q; }
        
        public String getA() { return a; }
        public void setA(String a) { this.a = a; }
        
        public String getH() { return h; }
        public void setH(String h) { this.h = h; }
    }
    
    // קלאס למשפט
    public static class Quote {
        private String text;
        private String author;
        
        public Quote(String text, String author) {
            this.text = text;
            this.author = author;
        }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        @Override
        public String toString() {
            return "\"" + text + "\" - " + author;
        }
    }
}