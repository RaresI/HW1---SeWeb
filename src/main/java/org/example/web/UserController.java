package org.example.web;

import jakarta.validation.Valid;
import org.example.model.User;
import org.example.repo.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @ModelAttribute("skillLevels")
    public java.util.List<String> skillLevels() {
        return User.SKILL_LEVELS;
    }

    @ModelAttribute("preferredCuisines")
    public java.util.List<String> preferredCuisines() {
        return User.PREFERRED_CUISINES;
    }

    @GetMapping("/users")
    public String list(Model model) {
        model.addAttribute("users", users.findAll());
        return "users";
    }

    @GetMapping("/users/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "user-form";
    }

    @PostMapping("/users")
    public String create(@Valid @ModelAttribute("user") User user,
                         BindingResult binding,
                         RedirectAttributes flash) throws Exception {
        if (binding.hasErrors()) {
            return "user-form";
        }
        User saved = users.save(user);
        flash.addFlashAttribute("savedName", saved.getName() + " " + saved.getSurname());
        return "redirect:/users";
    }
}
