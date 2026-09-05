package com.acadconf.controller;

import com.acadconf.model.Utilisateur;
import com.acadconf.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("error", "Email ou mot de passe incorrect.");
        if (logout != null) model.addAttribute("message", "Vous avez été déconnecté.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("utilisateur", new Utilisateur());
        model.addAttribute("roles", List.of(Utilisateur.Role.AUTEUR));
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("utilisateur") Utilisateur utilisateur,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("roles", List.of(Utilisateur.Role.AUTEUR));
            return "auth/register";
        }
        try {
            authService.register(utilisateur);
            redirectAttributes.addFlashAttribute("success", "Compte créé avec succès. Vous pouvez vous connecter.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", List.of(Utilisateur.Role.AUTEUR));
            return "auth/register";
        }
    }
}
