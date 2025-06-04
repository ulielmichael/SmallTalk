package mu.smalltalk.entitis;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "groups")
public class Group 
{
    @Id
    private String name;
    private ArrayList<String> users;   // emails(user id) list
    // private ArrayList memberIds;
    
    public Group() { }

    public Group(String name) 
    {
        this.name = name;
        this.users = new ArrayList<>();
    }

    public void addUserId(String userId)
    {
        users.add(userId);
    }

    public String getName() {
        return name;
    }
    public String getId() {
        return this.name;
    }

    
    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<String> users) {
        this.users = users;
    }
    //    public List<String> getMemberIds() {
    //     if (memberIds == null) {
    //         memberIds = new ArrayList<>();
    //     }
    //     return getMemberIds();
    // }

    @Override
    public String toString() {
        return "Group [name=" + name + ", users=" + users + "]";
    }

    

 
}
