package mu.smalltalk.Controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import mu.smalltalk.Services.MongoDbSerivce;
import mu.smalltalk.entitis.Group;

@Controller
public class chatController {
    
    private final MongoDbSerivce mongoDbService;
    
    public chatController(MongoDbSerivce mongoDbService) {
        this.mongoDbService = mongoDbService;
    }
    
    @GetMapping("/chat")
    public String showChatPage(Model model, Authentication authentication) {
        String userId = authentication.getName(); 
        
        List<Group> userGroups = mongoDbService.getUserGroups(userId);
        
        model.addAttribute("userGroups", userGroups);
        
        return "chat"; 
    }
}
