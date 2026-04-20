package org.example.web;

import jakarta.validation.Valid;
import org.example.model.Recipe;
import org.example.repo.RecipeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RecipeController {

    private final RecipeRepository recipes;

    public RecipeController(RecipeRepository recipes) {
        this.recipes = recipes;
    }

    @ModelAttribute("cuisines")
    public java.util.List<String> cuisines() {
        return Recipe.CUISINES;
    }

    @ModelAttribute("difficulties")
    public java.util.List<String> difficulties() {
        return Recipe.DIFFICULTIES;
    }

    @GetMapping({"/", "/recipes"})
    public String list(Model model) {
        model.addAttribute("recipes", recipes.findAll());
        return "recipes";
    }

    @GetMapping("/recipes/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("recipe")) {
            model.addAttribute("recipe", new Recipe());
        }
        return "recipe-form";
    }

    @PostMapping("/recipes")
    public String create(@Valid @ModelAttribute("recipe") Recipe recipe,
                         BindingResult binding,
                         RedirectAttributes flash) throws Exception {
        if (binding.hasErrors()) {
            return "recipe-form";
        }
        Recipe saved = recipes.save(recipe);
        flash.addFlashAttribute("savedTitle", saved.getTitle());
        return "redirect:/recipes";
    }
}
