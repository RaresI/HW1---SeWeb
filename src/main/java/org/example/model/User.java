package org.example.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class User {

    public static final java.util.List<String> SKILL_LEVELS = Recipe.DIFFICULTIES;
    public static final java.util.List<String> PREFERRED_CUISINES = Recipe.CUISINES;

    private String id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be 100 characters or fewer")
    private String name;

    @NotBlank(message = "Surname is required")
    @Size(max = 100, message = "Surname must be 100 characters or fewer")
    private String surname;

    @NotBlank(message = "Skill level is required")
    @Pattern(regexp = "Easy|Medium|Hard", message = "Skill level must be Easy, Medium, or Hard")
    private String skillLevel;

    @NotBlank(message = "Preferred cuisine is required")
    @Pattern(regexp = "Italian|Asian", message = "Preferred cuisine must be Italian or Asian")
    private String preferredCuisine;

    public User() {}

    public User(String id, String name, String surname, String skillLevel, String preferredCuisine) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.skillLevel = skillLevel;
        this.preferredCuisine = preferredCuisine;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public String getPreferredCuisine() { return preferredCuisine; }
    public void setPreferredCuisine(String preferredCuisine) { this.preferredCuisine = preferredCuisine; }
}
