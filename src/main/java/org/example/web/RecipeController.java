package org.example.web;

import jakarta.validation.Valid;
import org.example.model.Recipe;
import org.example.repo.RecipeRepository;
import org.example.repo.UserRepository;
import org.example.service.RecipeDetailsService;
import org.example.service.RecipeXslRenderService;
import org.example.service.RecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RecipeController {

    private final RecipeRepository recipes;
    private final UserRepository users;
    private final RecipeXslRenderService recipeXslRenderService;
    private final RecipeDetailsService recipeDetailsService;
    private final RecommendationService recommendationService;

    public RecipeController(RecipeRepository recipes,
                            UserRepository users,
                            RecipeXslRenderService recipeXslRenderService,
                            RecipeDetailsService recipeDetailsService,
                            RecommendationService recommendationService) {
        this.recipes = recipes;
        this.users = users;
        this.recipeXslRenderService = recipeXslRenderService;
        this.recipeDetailsService = recipeDetailsService;
        this.recommendationService = recommendationService;
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
    public String list(@RequestParam(value = "userId", required = false) String userId,
                       Model model) throws Exception {
        java.util.List<org.example.model.User> allUsers = users.findAll();
        org.example.model.User selectedUser = users.findById(userId).orElse(allUsers.isEmpty() ? null : allUsers.get(0));
        String selectedSkill = selectedUser == null ? "" : selectedUser.getSkillLevel();
        String renderedTable = recipeXslRenderService.render(recipes.findAll(), selectedSkill);
        model.addAttribute("usersForSelection", allUsers);
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("selectedUserId", selectedUser == null ? "" : selectedUser.getId());
        model.addAttribute("selectedSkill", selectedSkill);
        model.addAttribute("recipesTableHtml", renderedTable);
        return "recipes";
    }

    @GetMapping("/recipes/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("recipe")) {
            model.addAttribute("recipe", new Recipe());
        }
        return "recipe-form";
    }

    @GetMapping("/recipes/{id:r[0-9]+}")
    public String details(@PathVariable("id") String id, Model model) throws Exception {
        model.addAttribute("recipe", recipeDetailsService.findById(id).orElse(null));
        return "recipe-details";
    }

    @GetMapping("/recipes/cuisine")
    public String cuisineFilter(@RequestParam(value = "cuisine", required = false) String cuisine,
                                Model model) throws Exception {
        String selectedCuisine = (cuisine == null || cuisine.isBlank()) ? "" : cuisine;
        model.addAttribute("selectedCuisine", selectedCuisine);
        if (!selectedCuisine.isEmpty() && Recipe.CUISINES.contains(selectedCuisine)) {
            model.addAttribute("recipesByCuisine", recommendationService.recipesByCuisine(selectedCuisine));
        } else {
            model.addAttribute("recipesByCuisine", java.util.List.of());
        }
        return "recipes-by-cuisine";
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
