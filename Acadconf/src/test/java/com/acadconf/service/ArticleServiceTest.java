package com.acadconf.service;

import com.acadconf.model.Article;
import com.acadconf.model.Conference;
import com.acadconf.model.Utilisateur;
import com.acadconf.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleService - Tests unitaires")
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private MultipartFile fichierMock;

    @InjectMocks
    private ArticleService articleService;

    @TempDir
    Path tempDir;

    private Conference confOuverte;
    private Conference confFermee;
    private Utilisateur auteur;
    private Article article;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(articleService, "uploadDir", tempDir.toString());

        auteur = new Utilisateur();
        auteur.setId(1L);
        auteur.setNom("Akki");
        auteur.setPrenom("Imran");

        confOuverte = new Conference();
        confOuverte.setId(1L);
        confOuverte.setTitre("ICSE 2026");
        confOuverte.setStatut(Conference.Statut.OUVERTE);
        confOuverte.setDateLimiteSoumission(LocalDate.now().plusDays(30));
        confOuverte.setDateLimiteEvaluation(LocalDate.now().plusDays(60));

        confFermee = new Conference();
        confFermee.setId(2L);
        confFermee.setTitre("NeurIPS 2024");
        confFermee.setStatut(Conference.Statut.OUVERTE);
        confFermee.setDateLimiteSoumission(LocalDate.now().minusDays(1));
        confFermee.setDateLimiteEvaluation(LocalDate.now().plusDays(10));

        article = new Article();
        article.setId(10L);
        article.setTitre("Mon article de recherche");
        article.setConference(confOuverte);
    }

    // ------------------------------------------------------------------ submit

    @Test
    @DisplayName("submit - leve exception si la deadline de soumission est depassee")
    void submit_leveExceptionSiDeadlineDepassee() {
        assertThatThrownBy(() -> articleService.submit(article, fichierMock, confFermee, auteur))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("date limite de soumission");
    }

    @Test
    @DisplayName("submit - leve exception si le fichier n'est pas un PDF")
    void submit_leveExceptionSiFichierNonPdf() {
        when(fichierMock.isEmpty()).thenReturn(false);
        when(fichierMock.getContentType()).thenReturn("application/msword");

        assertThatThrownBy(() -> articleService.submit(article, fichierMock, confOuverte, auteur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    @DisplayName("submit - leve exception si le fichier est vide")
    void submit_leveExceptionSiFichierVide() {
        when(fichierMock.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> articleService.submit(article, fichierMock, confOuverte, auteur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fichier");
    }

    @Test
    @DisplayName("submit - leve exception si le fichier depasse 10 Mo")
    void submit_leveExceptionSiFichierTropGrand() {
        when(fichierMock.isEmpty()).thenReturn(false);
        when(fichierMock.getContentType()).thenReturn("application/pdf");
        when(fichierMock.getSize()).thenReturn(11_000_000L);

        assertThatThrownBy(() -> articleService.submit(article, fichierMock, confOuverte, auteur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taille");
    }

    @Test
    @DisplayName("submit - soumet l'article avec succes si toutes les conditions sont remplies")
    void submit_creeArticleAvecSucces() throws Exception {
        when(fichierMock.isEmpty()).thenReturn(false);
        when(fichierMock.getContentType()).thenReturn("application/pdf");
        when(fichierMock.getSize()).thenReturn(500_000L);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        Article result = articleService.submit(article, fichierMock, confOuverte, auteur);

        assertThat(result.getAuteurPrincipal()).isEqualTo(auteur);
        assertThat(result.getConference()).isEqualTo(confOuverte);
        assertThat(result.getFichierPdf()).isNotNull().endsWith(".pdf");
        verify(articleRepository).save(article);
    }

    // ------------------------------------------------------------------ update

    @Test
    @DisplayName("update - leve exception si la deadline est depassee lors de la modification")
    void update_leveExceptionSiDeadlineDepassee() {
        Article articleFerme = new Article();
        articleFerme.setId(20L);
        articleFerme.setTitre("Article ferme");
        articleFerme.setConference(confFermee);

        when(articleRepository.findById(20L)).thenReturn(Optional.of(articleFerme));

        Article modification = new Article();
        modification.setId(20L);
        modification.setTitre("Nouveau titre");

        assertThatThrownBy(() -> articleService.update(modification, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Modification impossible");
    }

    @Test
    @DisplayName("update - met a jour titre, resume et mots-cles sans nouveau fichier")
    void update_metsAJourChampsTextuels() throws Exception {
        Article existing = new Article();
        existing.setId(10L);
        existing.setTitre("Ancien titre");
        existing.setConference(confOuverte);

        when(articleRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(articleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Article modification = new Article();
        modification.setId(10L);
        modification.setTitre("Nouveau titre");
        modification.setResume("Nouveau resume");
        modification.setMotsCles("ML, IA");

        Article result = articleService.update(modification, null);

        assertThat(result.getTitre()).isEqualTo("Nouveau titre");
        assertThat(result.getResume()).isEqualTo("Nouveau resume");
        assertThat(result.getMotsCles()).isEqualTo("ML, IA");
        verify(articleRepository).save(existing);
    }

    // ------------------------------------------------------------------ findById

    @Test
    @DisplayName("findById - leve exception si l'article est introuvable")
    void findById_leveExceptionSiInexistant() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("introuvable");
    }

    // ------------------------------------------------------------------ findByAuteur

    @Test
    @DisplayName("findByAuteur - retourne les articles de l'auteur")
    void findByAuteur_retourneArticlesDeLAuteur() {
        when(articleRepository.findByAuteurPrincipal(auteur)).thenReturn(List.of(article));

        List<Article> result = articleService.findByAuteur(auteur);

        assertThat(result).hasSize(1);
        verify(articleRepository).findByAuteurPrincipal(auteur);
    }

    // ------------------------------------------------------------------ updateStatut

    @Test
    @DisplayName("updateStatut - change le statut de l'article et sauvegarde")
    void updateStatut_changeStatutEtSauvegarde() {
        article.setStatutArticle(Article.Statut.SOUMIS);
        when(articleRepository.findById(10L)).thenReturn(Optional.of(article));
        when(articleRepository.save(article)).thenReturn(article);

        articleService.updateStatut(10L, Article.Statut.ACCEPTE);

        assertThat(article.getStatutArticle()).isEqualTo(Article.Statut.ACCEPTE);
        verify(articleRepository).save(article);
    }

    // ------------------------------------------------------------------ delete

    @Test
    @DisplayName("delete - appelle deleteById sur le repository")
    void delete_appelleDeleteById() {
        when(articleRepository.findById(10L)).thenReturn(Optional.of(article));

        articleService.delete(10L);

        verify(articleRepository).deleteById(10L);
    }
}
