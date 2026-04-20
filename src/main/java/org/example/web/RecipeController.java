package org.example.web;

import org.example.repo.RecipeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RecipeController {

    private final RecipeRepository recipes;

    public RecipeController(RecipeRepository recipes) {
        this.recipes = recipes;
    }

    @GetMapping({"/", "/recipes"})
    public String list(Model model) {
        model.addAttribute("recipes", recipes.findAll());
        return "recipes";
    }
}
