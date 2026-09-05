package com.acadconf.service;

import com.acadconf.model.Utilisateur;
import com.acadconf.repository.UtilisateurRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));

        if (user.getStatutCompte() == Utilisateur.StatutCompte.EN_ATTENTE) {
            throw new UsernameNotFoundException("Votre compte est en attente d'approbation.");
        }
        if (user.getStatutCompte() != Utilisateur.StatutCompte.ACTIF) {
            throw new UsernameNotFoundException("Compte désactivé ou bloqué.");
        }

        return new User(
                user.getEmail(),
                user.getMotDePasse(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    public Utilisateur register(Utilisateur utilisateur) {
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            throw new IllegalArgumentException("Cette adresse e-mail est déjà utilisée.");
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur update(Long id, Utilisateur data, String newPassword) {
        Utilisateur existing = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        existing.setNom(data.getNom());
        existing.setPrenom(data.getPrenom());
        existing.setEmail(data.getEmail());
        existing.setRole(data.getRole());
        existing.setAffiliation(data.getAffiliation());
        existing.setPays(data.getPays());
        existing.setStatutCompte(data.getStatutCompte());
        if (newPassword != null && !newPassword.isBlank()) {
            existing.setMotDePasse(passwordEncoder.encode(newPassword));
        }
        return utilisateurRepository.save(existing);
    }

    public Utilisateur toggleAccountStatus(Long userId) {
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        Utilisateur.StatutCompte next = switch (user.getStatutCompte()) {
            case ACTIF      -> Utilisateur.StatutCompte.BLOQUE;
            case BLOQUE     -> Utilisateur.StatutCompte.ACTIF;
            case EN_ATTENTE -> Utilisateur.StatutCompte.ACTIF;
            case INACTIF    -> Utilisateur.StatutCompte.ACTIF;
        };
        user.setStatutCompte(next);
        return utilisateurRepository.save(user);
    }

    public Utilisateur setStatusEnAttente(Long userId) {
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setStatutCompte(Utilisateur.StatutCompte.EN_ATTENTE);
        return utilisateurRepository.save(user);
    }

    public Utilisateur findByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));
    }
}
