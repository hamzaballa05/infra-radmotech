package com.acadconf.service;

import com.acadconf.model.Utilisateur;
import com.acadconf.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests unitaires")
class AuthServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setNom("Akki");
        utilisateur.setPrenom("Imran");
        utilisateur.setEmail("auteur@acadconf.ma");
        utilisateur.setMotDePasse("encoded_1234");
        utilisateur.setRole(Utilisateur.Role.AUTEUR);
        utilisateur.setStatutCompte(Utilisateur.StatutCompte.ACTIF);
    }

    // ------------------------------------------------------------------ loadUserByUsername

    @Test
    @DisplayName("loadUserByUsername - retourne UserDetails si compte actif")
    void loadUserByUsername_retourneUserDetailsSiActif() {
        when(utilisateurRepository.findByEmail("auteur@acadconf.ma")).thenReturn(Optional.of(utilisateur));

        var details = authService.loadUserByUsername("auteur@acadconf.ma");

        assertThat(details.getUsername()).isEqualTo("auteur@acadconf.ma");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_AUTEUR"));
    }

    @Test
    @DisplayName("loadUserByUsername - leve exception si compte EN_ATTENTE")
    void loadUserByUsername_leveExceptionSiEnAttente() {
        utilisateur.setStatutCompte(Utilisateur.StatutCompte.EN_ATTENTE);
        when(utilisateurRepository.findByEmail(anyString())).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> authService.loadUserByUsername("auteur@acadconf.ma"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("attente");
    }

    @Test
    @DisplayName("loadUserByUsername - leve exception si compte BLOQUE")
    void loadUserByUsername_leveExceptionSiBloque() {
        utilisateur.setStatutCompte(Utilisateur.StatutCompte.BLOQUE);
        when(utilisateurRepository.findByEmail(anyString())).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> authService.loadUserByUsername("auteur@acadconf.ma"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ------------------------------------------------------------------ register

    @Test
    @DisplayName("register - encode le mot de passe avant sauvegarde")
    void register_encodeMotDePasse() {
        utilisateur.setMotDePasse("1234");
        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("encoded_1234");
        when(utilisateurRepository.save(any())).thenReturn(utilisateur);

        authService.register(utilisateur);

        verify(passwordEncoder).encode("1234");
        assertThat(utilisateur.getMotDePasse()).isEqualTo("encoded_1234");
    }

    @Test
    @DisplayName("register - leve exception si l'email est deja utilise")
    void register_leveExceptionSiEmailDuplique() {
        when(utilisateurRepository.existsByEmail("auteur@acadconf.ma")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(utilisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà utilisée");
    }

    // ------------------------------------------------------------------ toggleAccountStatus

    @Test
    @DisplayName("toggleAccountStatus - ACTIF devient BLOQUE")
    void toggleAccountStatus_actifDevientBloque() {
        utilisateur.setStatutCompte(Utilisateur.StatutCompte.ACTIF);
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any())).thenReturn(utilisateur);

        authService.toggleAccountStatus(1L);

        assertThat(utilisateur.getStatutCompte()).isEqualTo(Utilisateur.StatutCompte.BLOQUE);
    }

    @Test
    @DisplayName("toggleAccountStatus - BLOQUE devient ACTIF")
    void toggleAccountStatus_bloqueDevientActif() {
        utilisateur.setStatutCompte(Utilisateur.StatutCompte.BLOQUE);
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any())).thenReturn(utilisateur);

        authService.toggleAccountStatus(1L);

        assertThat(utilisateur.getStatutCompte()).isEqualTo(Utilisateur.StatutCompte.ACTIF);
    }

    @Test
    @DisplayName("toggleAccountStatus - EN_ATTENTE devient ACTIF")
    void toggleAccountStatus_enAttenteDevientActif() {
        utilisateur.setStatutCompte(Utilisateur.StatutCompte.EN_ATTENTE);
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any())).thenReturn(utilisateur);

        authService.toggleAccountStatus(1L);

        assertThat(utilisateur.getStatutCompte()).isEqualTo(Utilisateur.StatutCompte.ACTIF);
    }
}
