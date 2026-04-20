package org.example.model;

public class Recipe {

    private String id;
    private String title;
    private String cuisine;
    private String difficulty;
    private String source;

    public Recipe() {}

    public Recipe(String id, String title, String cuisine, String difficulty, String source) {
        this.id = id;
        this.title = title;
        this.cuisine = cuisine;
        this.difficulty = difficulty;
        this.source = source;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
