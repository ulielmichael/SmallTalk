package mu.smalltalk.entitis;



import java.util.UUID;

public class Group {
    private String id;
    private String name;
    private String description;
    private String createdBy;
    private long creationTimestamp;
    
    // Default constructor
    public Group() {
        this.id = UUID.randomUUID().toString();
        this.creationTimestamp = System.currentTimeMillis();
    }
    
    // Constructor with name and description
    public Group(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    // Constructor with all fields
    public Group(String id, String name, String description, String createdBy, long creationTimestamp) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.creationTimestamp = creationTimestamp;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public long getCreationTimestamp() {
        return creationTimestamp;
    }
    
    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return id.equals(group.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "Group{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}