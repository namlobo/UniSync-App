package model.resource;

// this is used for resource classification. It follows encapsulation and information expert. 
// Encapsulation because the fields are private and can only be accessed through getters
// Information expert because the category owns category values so it converts itself to json 
public class Category {

    private int categoryId;
    private String mainType;
    private String subType;
    private String description;

    public Category(int categoryId, String mainType, String subType) {
        this.categoryId = categoryId;
        this.mainType = mainType;
        this.subType = subType;
        this.description = "";
    }

    public Category(int categoryId, String mainType, String subType, String description) {
        this.categoryId = categoryId;
        this.mainType = mainType;
        this.subType = subType;
        this.description = description;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getMainType() {
        return mainType;
    }

    public String getSubType() {
        return subType;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        if (subType == null || subType.isBlank()) return mainType;
        return mainType + " - " + subType;
    }

    public org.json.JSONObject toJson() {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("categoryId", getCategoryId());
        json.put("mainType", getMainType());
        json.put("subType", getSubType());
        json.put("description", getDescription());
        return json;
    }
}