
package mu.smalltalk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {
        String username = "ulilelmichael";
        String password = encodeURIComponent("Michaelis2k"); 
        return new SimpleMongoClientDatabaseFactory(
            "mongodb+srv://" + username + ":" + password + "@cluster0.hfb0biz.mongodb.net/smalltalk?retryWrites=true&w=majority&appName=Cluster0"
        );
    }
    
    private String encodeURIComponent(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8")
                .replaceAll("\\+", "%20")
                .replaceAll("\\%21", "!")
                .replaceAll("\\%27", "'")
                .replaceAll("\\%28", "(")
                .replaceAll("\\%29", ")")
                .replaceAll("\\%7E", "~");
        } catch (java.io.UnsupportedEncodingException e) {
            return s;
        }
    }
    
}    