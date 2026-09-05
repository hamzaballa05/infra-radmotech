package com.acadconf.controller;

import com.acadconf.model.Utilisateur;
import com.acadconf.repository.UtilisateurRepository;
import com.acadconf.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UtilisateurRepository utilisateurRepository;
    private final AuthService authService;

    public AdminController(UtilisateurRepository utilisateurRepository, AuthService authService) {
        this.utilisateurRepository = utilisateurRepository;
        this.authService = authService;
    }

    private static final int PAGE_SIZE = 10;

    @GetMapping("/users")
    public String users(@AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(defaultValue = "") String search,
                        @RequestParam(required = false) String role,
                        @RequestParam(required = false) String statut,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        model.addAttribute("user", authService.findByEmail(userDetails.getUsername()));

        Utilisateur.Role roleEnum = (role != null && !role.isBlank()) ? Utilisateur.Role.valueOf(role) : null;
        Utilisateur.StatutCompte statutEnum = (statut != null && !statut.isBlank()) ? Utilisateur.StatutCompte.valueOf(statut) : null;

        Page<Utilisateur> pageResult = utilisateurRepository.findFiltered(
            search.isBlank() ? null : search,
            roleEnum,
            statutEnum,
            PageRequest.of(page, PAGE_SIZE, Sort.by("id"))
        );

        // Stats always from full set
        long total = utilisateurRepository.count();
        long actifs = utilisateurRepository.findByStatutCompte(Utilisateur.StatutCompte.ACTIF).size();
        long presidents = utilisateurRepository.findByRole(Utilisateur.Role.PRESIDENT).size();
        long bloques = utilisateurRepository.findByStatutCompte(Utilisateur.StatutCompte.BLOQUE).size();

        model.addAttribute("utilisateurs", pageResult.getContent());
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("currentPage", page);
        model.addAttribute("search", search);
        model.addAttribute("roleFilter", role != null ? role : "");
        model.addAttribute("statutFilter", statut != null ? statut : "");
        model.addAttribute("totalUtilisateurs", total);
        model.addAttribute("totalActifs", actifs);
        model.addAttribute("totalPresidents", presidents);
        model.addAttribute("totalBloques", bloques);
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("user", authService.findByEmail(userDetails.getUsername()));
        model.addAttribute("utilisateur", new Utilisateur());
        model.addAttribute("roles", Utilisateur.Role.values());
        model.addAttribute("statuts", Utilisateur.StatutCompte.values());
        model.addAttribute("editMode", false);
        return "admin/user-form";
    }

    @PostMapping("/users/new")
    public String createUser(@ModelAttribute("utilisateur") Utilisateur utilisateur,
                             @RequestParam("motDePasseNew") String motDePasse,
                             RedirectAttributes redirectAttributes) {
        try {
            utilisateur.setMotDePasse(motDePasse);
            authService.register(utilisateur);
            redirectAttributes.addFlashAttribute("success", "Utilisateur créé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/new";
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        model.addAttribute("user", authService.findByEmail(userDetails.getUsername()));
        model.addAttribute("utilisateur", utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable")));
        model.addAttribute("roles", Utilisateur.Role.values());
        model.addAttribute("statuts", Utilisateur.StatutCompte.values());
        model.addAttribute("editMode", true);
        return "admin/user-form";
    }

    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute("utilisateur") Utilisateur utilisateur,
                             @RequestParam(value = "motDePasseNew", required = false) String motDePasse,
                             RedirectAttributes redirectAttributes) {
        try {
            authService.update(id, utilisateur, motDePasse);
            redirectAttributes.addFlashAttribute("success", "Utilisateur mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleStatut(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            authService.toggleAccountStatus(id);
            redirectAttributes.addFlashAttribute("success", "Statut du compte mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        utilisateurRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Utilisateur supprimé.");
        return "redirect:/admin/users";
    }
}
