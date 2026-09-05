package com.acadconf.service;

import com.acadconf.model.Conference;
import com.acadconf.model.Utilisateur;
import com.acadconf.repository.ConferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConferenceService - Tests unitaires")
class ConferenceServiceTest {

    @Mock
    private ConferenceRepository conferenceRepository;

    @InjectMocks
    private ConferenceService conferenceService;

    private Conference confOuverte;
    private Conference confExpiree;
    private Conference confFermee;

    @BeforeEach
    void setUp() {
        confOuverte = new Conference();
        confOuverte.setId(1L);
        confOuverte.setTitre("ICSE 2026");
        confOuverte.setStatut(Conference.Statut.OUVERTE);
        confOuverte.setDateLimiteSoumission(LocalDate.now().plusDays(30));
        confOuverte.setDateLimiteEvaluation(LocalDate.now().plusDays(60));

        confExpiree = new Conference();
        confExpiree.setId(2L);
        confExpiree.setTitre("NeurIPS 2024");
        confExpiree.setStatut(Conference.Statut.OUVERTE);
        confExpiree.setDateLimiteSoumission(LocalDate.now().minusDays(5));
        confExpiree.setDateLimiteEvaluation(LocalDate.now().minusDays(1));

        confFermee = new Conference();
        confFermee.setId(3L);
        confFermee.setTitre("ICLR 2025");
        confFermee.setStatut(Conference.Statut.FERMEE);
        confFermee.setDateLimiteSoumission(LocalDate.now().plusDays(10));
        confFermee.setDateLimiteEvaluation(LocalDate.now().plusDays(30));
    }

    // ------------------------------------------------------------------ findOuvertes

    @Test
    @DisplayName("findOuvertes - retourne uniquement les conferences ouvertes avec deadline future")
    void findOuvertes_retourneConferencesOuvertesEtNonExpirees() {
        when(conferenceRepository.findByStatut(Conference.Statut.OUVERTE))
                .thenReturn(List.of(confOuverte, confExpiree));

        List<Conference> result = conferenceService.findOuvertes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findOuvertes - retourne liste vide quand toutes les conferences sont expirees")
    void findOuvertes_retourneVideSiToutesExpirees() {
        when(conferenceRepository.findByStatut(Conference.Statut.OUVERTE))
                .thenReturn(List.of(confExpiree));

        List<Conference> result = conferenceService.findOuvertes();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findOuvertes - exclut les conferences fermees meme si deadline future")
    void findOuvertes_exclutConferencesFermees() {
        when(conferenceRepository.findByStatut(Conference.Statut.OUVERTE))
                .thenReturn(List.of());

        List<Conference> result = conferenceService.findOuvertes();

        assertThat(result).isEmpty();
        verify(conferenceRepository, never()).findByStatut(Conference.Statut.FERMEE);
    }

    // ------------------------------------------------------------------ findById

    @Test
    @DisplayName("findById - retourne la conference quand elle existe")
    void findById_retourneConferenceSiExiste() {
        when(conferenceRepository.findById(1L)).thenReturn(Optional.of(confOuverte));

        Conference result = conferenceService.findById(1L);

        assertThat(result.getTitre()).isEqualTo("ICSE 2026");
    }

    @Test
    @DisplayName("findById - leve une exception si la conference est introuvable")
    void findById_leveExceptionSiInexistante() {
        when(conferenceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conferenceService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("introuvable");
    }

    // ------------------------------------------------------------------ save

    @Test
    @DisplayName("save - delègue au repository et retourne l'entite persistee")
    void save_delegueAuRepository() {
        when(conferenceRepository.save(confOuverte)).thenReturn(confOuverte);

        Conference result = conferenceService.save(confOuverte);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(conferenceRepository).save(confOuverte);
    }

    // ------------------------------------------------------------------ findByPresident

    @Test
    @DisplayName("findByPresident - retourne les conferences du president")
    void findByPresident_retourneConferencesCorrectement() {
        Utilisateur president = new Utilisateur();
        president.setId(10L);
        when(conferenceRepository.findByPresident(president)).thenReturn(List.of(confOuverte, confFermee));

        List<Conference> result = conferenceService.findByPresident(president);

        assertThat(result).hasSize(2);
        verify(conferenceRepository).findByPresident(president);
    }

    // ------------------------------------------------------------------ delete

    @Test
    @DisplayName("delete - appelle deleteById sur le repository")
    void delete_appelleDeleteById() {
        conferenceService.delete(1L);

        verify(conferenceRepository).deleteById(1L);
    }
}
