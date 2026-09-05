package com.acadconf.controller;

import com.acadconf.model.Utilisateur;
import com.acadconf.service.AuthService;
import com.acadconf.service.ArticleService;
import com.acadconf.service.ConferenceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final AuthService authService;
    private final ArticleService articleService;
    private final ConferenceService conferenceService;

    public DashboardController(AuthService authService, ArticleService articleService, ConferenceService conferenceService) {
        this.authService = authService;
        this.articleService = articleService;
        this.conferenceService = conferenceService;
    }

    @GetMapping("/auteur")
    public String auteurDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Utilisateur user = authService.findByEmail(userDetails.getUsername());
        var articles = articleService.findByAuteur(user);
        model.addAttribute("user", user);
        model.addAttribute("articles", articles);
        model.addAttribute("conferencesOuvertes", conferenceService.findOuvertes());
        model.addAttribute("totalSoumis", articles.size());
        model.addAttribute("totalAcceptes", articles.stream().filter(a -> a.getStatutArticle() == com.acadconf.model.Article.Statut.ACCEPTE).count());
        model.addAttribute("totalEnEvaluation", articles.stream().filter(a -> a.getStatutArticle() == com.acadconf.model.Article.Statut.EN_EVALUATION).count());
        return "dashboard/auteur";
    }

    @GetMapping("/president")
    public String presidentDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Utilisateur user = authService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("conferences", conferenceService.findByPresident(user));
        return "dashboard/president";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        return "redirect:/admin/users";
    }
}
