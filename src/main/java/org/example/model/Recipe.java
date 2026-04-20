package org.example.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Recipe {

    public static final java.util.List<String> CUISINES = java.util.List.of("Italian", "Asian");
    public static final java.util.List<String> DIFFICULTIES = java.util.List.of("Easy", "Medium", "Hard");

    private String id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be 200 characters or fewer")
    private String title;

    @NotBlank(message = "Cuisine is required")
    @Pattern(regexp = "Italian|Asian", message = "Cuisine must be Italian or Asian")
    private String cuisine;

    @NotBlank(message = "Difficulty is required")
    @Pattern(regexp = "Easy|Medium|Hard", message = "Difficulty must be Easy, Medium, or Hard")
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
