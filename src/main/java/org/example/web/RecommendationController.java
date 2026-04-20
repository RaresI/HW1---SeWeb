package org.example.web;

import org.example.model.Recipe;
import org.example.model.User;
import org.example.service.RecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class RecommendationController {

    private final RecommendationService recommendations;

    public RecommendationController(RecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    @GetMapping("/recommendations")
    public String show(Model model) throws Exception {
        Optional<User> user = recommendations.firstUser();
        model.addAttribute("user", user.orElse(null));
        if (user.isPresent()) {
            List<Recipe> matches = recommendations.recipesForSkillLevel(user.get().getSkillLevel());
            model.addAttribute("recipes", matches);
        } else {
            model.addAttribute("recipes", List.of());
        }
        return "recommendations";
    }
}
