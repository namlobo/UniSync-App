package model.resource;
import org.json.JSONObject;   
import org.json.JSONArray;   

import model.user.Student;

// Resource stores item state and behavior together, following SRP, Information Expert, Constructor Overloading (without price for lend/barter, with price for sell).
public class Resource {

    private int resourceId;
    private String title;
    private String description;
    private String condition;
    private ResourceStatus status;
    private ListingType listingType;
    private double price;
    private Student owner;
    private Category category;


    public Resource(int resourceId,
                    String title,
                    String description,
                    String condition,
                    ListingType listingType,
                    Student owner,
                    Category category) {

        this.resourceId = resourceId;
        this.title = title;
        this.description = description;
        this.condition = condition;
        this.listingType = listingType;
        this.owner = owner;
        this.category = category;
        this.status = ResourceStatus.AVAILABLE;
        this.price = 0.0;
    }

    public Resource(int resourceId,
                    String title,
                    String description,
                    String condition,
                    ListingType listingType,
                    double price,
                    Student owner,
                    Category category) {

        this.resourceId = resourceId;
        this.title = title;
        this.description = description;
        this.condition = condition;
        this.listingType = listingType;
        this.owner = owner;
        this.category = category;
        this.status = ResourceStatus.AVAILABLE;
        this.price = price;
    }

    public Resource(int resourceId,
                    String title,
                    String description,
                    String condition,
                    ResourceStatus status,
                    ListingType listingType,
                    double price,
                    Student owner,
                    Category category) {

        this.resourceId = resourceId;
        this.title = title;
        this.description = description;
        this.condition = condition;
        this.listingType = listingType;
        this.owner = owner;
        this.category = category;
        this.status = status;
        this.price = price;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getTitle() {
        return title;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public ListingType getListingType() {
        return listingType;
    }

    public Student getOwner() {
        return owner;
    }

    public Category getCategory() {
        return category;
    }

    // Updates the item's sold status inside the entity itself, which follows Information Expert.
    public void markSold() {
        status = ResourceStatus.SOLD;
    }

    // Updates the item's borrowed status in one place, supporting high cohesion.
    public void markBorrowed() {
        status = ResourceStatus.BORROWED;
    }

    // Reserves the item through domain behavior instead of external logic, supporting SRP.
    public void reserve() {
        status = ResourceStatus.RESERVED;
    }

    // Restores the item to available state inside the model, following Information Expert.
    public void makeAvailable() {
        status = ResourceStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == ResourceStatus.AVAILABLE;
    }
    public String getDescription() {
    return description;
}

public String getCondition() {
    return condition;
}

    public double getPrice() {
        return price;
    }
    // Converts the resource into JSON for APIs, keeping output formatting consistent across controllers.
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("resourceId", getResourceId());
        json.put("title", getTitle());
        json.put("description", getDescription());
        json.put("condition", getCondition());
        json.put("status", getStatus().toString());
        json.put("listingType", getListingType().toString());
        json.put("price", getPrice());
        json.put("owner", owner != null ? owner.toJson() : JSONObject.NULL);
        json.put("category", category != null ? category.toJson() : JSONObject.NULL);
        return json;
    }
}
