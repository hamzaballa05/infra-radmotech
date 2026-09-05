package com.acadconf.controller;

import com.acadconf.model.Conference;
import com.acadconf.model.Utilisateur;
import com.acadconf.service.AuthService;
import com.acadconf.service.ConferenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/conferences")
public class ConferenceController {

    private final ConferenceService conferenceService;
    private final AuthService authService;

    public ConferenceController(ConferenceService conferenceService, AuthService authService) {
        this.conferenceService = conferenceService;
        this.authService = authService;
    }

    private static final int PAGE_SIZE = 10;

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String statut,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("user", authService.findByEmail(userDetails.getUsername()));
        Conference.Statut statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try { statutEnum = Conference.Statut.valueOf(statut); } catch (IllegalArgumentException ignored) {}
        }
        var pageResult = conferenceService.findFiltered(search, statutEnum, page, PAGE_SIZE);
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("conferences", pageResult.getContent());
        model.addAttribute("search", search);
        model.addAttribute("statut", statut);
        return "conferences/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("user", authService.findByEmail(userDetails.getUsername()));
        model.addAttribute("conference", new Conference());
        return "conferences/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("conference") Conference conference,
                         BindingResult result,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "conferences/form";
        Utilisateur president = authService.findByEmail(userDetails.getUsername());
        conference.setPresident(president);
        conferenceService.save(conference);
        redirectAttributes.addFlashAttribute("success", "Conférence créée avec succès.");
        return "redirect:/conferences";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("user", authService.findByEmail(userDetails.getUsername()));
        model.addAttribute("conference", conferenceService.findById(id));
        return "conferences/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("user", authService.findByEmail(userDetails.getUsername()));
        model.addAttribute("conference", conferenceService.findById(id));
        return "conferences/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("conference") Conference conference,
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "conferences/form";
        conference.setId(id);
        conferenceService.save(conference);
        redirectAttributes.addFlashAttribute("success", "Conférence mise à jour.");
        return "redirect:/conferences";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        conferenceService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Conférence supprimée.");
        return "redirect:/conferences";
    }
}
