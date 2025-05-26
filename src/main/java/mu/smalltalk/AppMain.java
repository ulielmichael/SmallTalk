package mu.smalltalk;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import mu.smalltalk.Services.MongoDbSerivce;
import mu.smalltalk.entitis.User;

@SpringBootApplication
public class AppMain implements CommandLineRunner
{
    // // @Autowired
    // private MongoDbSerivce mongoDbSerivce;

    public static void main(String[] args) 
    {
        SpringApplication.run(AppMain.class, args);
        System.out.println(">>>>> AppMain running....");     
    }

    @Override
    public void run(String... args) throws Exception 
    {
        // User user1 = new User("ilanp@gmail.com","ilan peretz","1111");
        // User user2 = new User("michaell@gmail.com","michael uliel","2222");
        // User user3 = new User("danieli@gmail.com","daniel itzhak","3333");
        // User user4 = new User("elichayo@gmail.com","elichay oved","4444");

        // mongoDbSerivce.addUsertoDB(user1);
        // mongoDbSerivce.addUsertoDB(user2);
        // mongoDbSerivce.addUsertoDB(user3);
        // mongoDbSerivce.addUsertoDB(user4);
        

        // Group group1 = new Group("group1");
        // group1.addUserId("ilanp@gmail.com");
        // group1.addUserId("michaell@gmail.com");

        // Group group2 = new Group("group2");
        // group2.addUserId("ilanp@gmail.com");
        // group2.addUserId("danieli@gmail.com");
        // group2.addUserId("elichayo@gmail.com");
        // group2.addUserId("michaell@gmail.com");

        // Group group3 = new Group("group3");
        // group3.addUserId("michaell@gmail.com");
        // group3.addUserId("danieli@gmail.com");
        // group3.addUserId("elichayo@gmail.com");

        // mongoDbSerivce.addGroupToDB(group1);
        // mongoDbSerivce.addGroupToDB(group2);
        // mongoDbSerivce.addGroupToDB(group3);
        
    }
}
