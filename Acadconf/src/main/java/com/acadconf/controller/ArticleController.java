package com.acadconf.controller;

import com.acadconf.model.Article;
import com.acadconf.model.Conference;
import com.acadconf.model.Utilisateur;
import com.acadconf.repository.ArticleRepository;
import com.acadconf.service.ArticleService;
import com.acadconf.service.AuthService;
import com.acadconf.service.ConferenceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final AuthService authService;
    private final ConferenceService conferenceService;
    private final ArticleRepository articleRepository;

    public ArticleController(ArticleService articleService, AuthService authService,
                             ConferenceService conferenceService,
                             ArticleRepository articleRepository) {
        this.articleService = articleService;
        this.authService = authService;
        this.conferenceService = conferenceService;
        this.articleRepository = articleRepository;
    }

    private static final int PAGE_SIZE = 10;

    @GetMapping
    public String allArticles(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) String statut,
                              @RequestParam(required = false) Long conferenceId,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        Utilisateur user = authService.findByEmail(userDetails.getUsername());
        Article.Statut statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try { statutEnum = Article.Statut.valueOf(statut); } catch (IllegalArgumentException ignored) {}
        }
        var pageResult = articleService.findAllFiltered(search, statutEnum, conferenceId, page, PAGE_SIZE);
        // Global stats always reflect full dataset
        var allArticles = articleService.findAll();
        model.addAttribute("user", user);
        model.addAttribute("articles", pageResult.getContent());
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("search", search);
        model.addAttribute("statut", statut);
        model.addAttribute("conferenceId", conferenceId);
        model.addAttribute("conferences", conferenceService.findAll());
        model.addAttribute("totalSoumis", allArticles.stream().filter(a -> a.getStatutArticle() == Article.Statut.SOUMIS).count());
        model.addAttribute("totalEnEvaluation", allArticles.stream().filter(a -> a.getStatutArticle() == Article.Statut.EN_EVALUATION).count());
        model.addAttribute("totalAcceptes", allArticles.stream().filter(a -> a.getStatutArticle() == Article.Statut.ACCEPTE).count());
        model.addAttribute("totalRejetes", allArticles.stream().filter(a -> a.getStatutArticle() == Article.Statut.REJETE).count());
        return "articles/list";
    }

    @GetMapping("/my")
    public String myArticles(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(required = false) String search,
                             @RequestParam(required = false) String statut,
                             @RequestParam(required = false) Long conferenceId,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        Utilisateur user = authService.findByEmail(userDetails.getUsername());
        Article.Statut statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try { statutEnum = Article.Statut.valueOf(statut); } catch (IllegalArgumentException ignored) {}
        }
        var pageResult = articleService.findByAuteurFiltered(user, search, statutEnum, conferenceId, page, PAGE_SIZE);
        model.addAttribute("user", user);
        model.addAttribute("articles", pageResult.getContent());
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("search", search);
        model.addAttribute("statut", statut);
        model.addAttribute("conferenceId", conferenceId);
        model.addAttribute("conferences", conferenceService.findAll());
        return "articles/my-articles";
    }

    @GetMapping("/submit")
    public String submitForm(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(required = false) Long conferenceId,
                             Model model) {
        model.addAttribute("user", authService.findByEmail(userDetails.getUsername()));
        model.addAttribute("article", new Article());
        var conferences = conferenceService.findOuvertes();
        model.addAttribute("conferences", conferences);
        model.addAttribute("selectedConferenceId", conferenceId);
        return "articles/submit";
    }

    @PostMapping("/submit")
    public String submit(@ModelAttribute Article article,
                         @RequestParam("fichier") MultipartFile fichier,
                         @RequestParam("conferenceId") Long conferenceId,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            Utilisateur auteur = authService.findByEmail(userDetails.getUsername());
            articleService.submit(article, fichier, conferenceService.findById(conferenceId), auteur);
            redirectAttributes.addFlashAttribute("success", "Article soumis avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/articles/submit";
        }
        return "redirect:/articles/my";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Utilisateur user = authService.findByEmail(userDetails.getUsername());
        Article article = articleService.findById(id);
        model.addAttribute("user", user);
        model.addAttribute("article", article);
        return "articles/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("user", authService.findByEmail(userDetails.getUsername()));
        var article = articleService.findById(id);
        model.addAttribute("article", article);
        // Include all open conferences + the article's current conference (may be closed)
        var conferences = new java.util.ArrayList<>(conferenceService.findOuvertes());
        var current = article.getConference();
        if (conferences.stream().noneMatch(c -> c.getId().equals(current.getId()))) {
            conferences.add(0, current);
        }
        model.addAttribute("conferences", conferences);
        model.addAttribute("selectedConferenceId", current.getId());
        return "articles/submit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        Article article = articleService.findById(id);
        Utilisateur user = authService.findByEmail(userDetails.getUsername());
        if (!article.getAuteurPrincipal().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Vous n'êtes pas autorisé à supprimer cet article.");
            return "redirect:/articles/my";
        }
        if (article.getStatutArticle() != Article.Statut.SOUMIS) {
            redirectAttributes.addFlashAttribute("error", "Seuls les articles en statut SOUMIS peuvent être supprimés.");
            return "redirect:/articles/my";
        }
        articleService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Article supprimé avec succès.");
        return "redirect:/articles/my";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute Article article,
                         @RequestParam(value = "fichier", required = false) MultipartFile fichier,
                         RedirectAttributes redirectAttributes) {
        article.setId(id);
        try {
            articleService.update(article, fichier);
            redirectAttributes.addFlashAttribute("success", "Article mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/articles/my";
    }

    @GetMapping("/accepted")
    public String acceptedArticles(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam(required = false) Long conferenceId,
                                    Model model) {
        Utilisateur user = authService.findByEmail(userDetails.getUsername());
        List<Article> accepted;
        if (conferenceId != null) {
            Conference conf = conferenceService.findById(conferenceId);
            accepted = articleRepository.findByConferenceAndStatutArticle(conf, Article.Statut.ACCEPTE);
        } else {
            accepted = articleRepository.findByStatutArticle(Article.Statut.ACCEPTE);
        }
        model.addAttribute("user", user);
        model.addAttribute("articles", accepted);
        model.addAttribute("conferences", conferenceService.findAll());
        model.addAttribute("conferenceId", conferenceId);
        return "articles/accepted";
    }
}
