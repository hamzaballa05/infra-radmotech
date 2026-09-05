package com.acadconf.service;

import com.acadconf.model.Article;
import com.acadconf.model.Conference;
import com.acadconf.model.Utilisateur;
import com.acadconf.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<Article> findByAuteur(Utilisateur auteur) {
        return articleRepository.findByAuteurPrincipal(auteur);
    }

    public Page<Article> findByAuteurFiltered(Utilisateur auteur, String search, Article.Statut statut,
                                              Long conferenceId, int page, int size) {
        return articleRepository.findByAuteurFiltered(
                auteur,
                search == null || search.isBlank() ? null : search,
                statut,
                conferenceId,
                PageRequest.of(page, size, Sort.by("dateSoumission").descending())
        );
    }

    public List<Article> findByConference(Conference conference) {
        return articleRepository.findByConference(conference);
    }

    public Article findById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable"));
    }

    public Article submit(Article article, MultipartFile fichier, Conference conference, Utilisateur auteur) throws IOException {
        if (!conference.isSubmissionOpen()) {
            throw new IllegalStateException("La date limite de soumission est dépassée.");
        }
        validatePdf(fichier);

        String filename = UUID.randomUUID() + ".pdf";
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        fichier.transferTo(uploadPath.resolve(filename));

        article.setFichierPdf(filename);
        article.setConference(conference);
        article.setAuteurPrincipal(auteur);
        return articleRepository.save(article);
    }

    public Article update(Article article, MultipartFile fichier) throws IOException {
        Article existing = findById(article.getId());
        if (!existing.getConference().isSubmissionOpen()) {
            throw new IllegalStateException("La date limite de soumission est dépassée. Modification impossible.");
        }
        existing.setTitre(article.getTitre());
        existing.setResume(article.getResume());
        existing.setMotsCles(article.getMotsCles());

        if (fichier != null && !fichier.isEmpty()) {
            validatePdf(fichier);
            deletePdf(existing.getFichierPdf());
            String filename = UUID.randomUUID() + ".pdf";
            fichier.transferTo(Paths.get(uploadDir).resolve(filename));
            existing.setFichierPdf(filename);
        }
        return articleRepository.save(existing);
    }

    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    public Page<Article> findAllFiltered(String search, Article.Statut statut, Long conferenceId, int page, int size) {
        return articleRepository.findAllFiltered(
                search == null || search.isBlank() ? null : search,
                statut,
                conferenceId,
                PageRequest.of(page, size, Sort.by("dateSoumission").descending())
        );
    }

    public void delete(Long id) {
        Article article = findById(id);
        deletePdf(article.getFichierPdf());
        articleRepository.deleteById(id);
    }

    private void deletePdf(String fichierPdf) {
        if (fichierPdf == null) return;
        try { Files.deleteIfExists(Paths.get(uploadDir).resolve(fichierPdf)); } catch (IOException ignored) {}
    }

    public void updateStatut(Long articleId, Article.Statut statut) {
        Article article = findById(articleId);
        article.setStatutArticle(statut);
        articleRepository.save(article);
    }

    private void validatePdf(MultipartFile fichier) {
        if (fichier.isEmpty()) throw new IllegalArgumentException("Aucun fichier sélectionné.");
        String contentType = fichier.getContentType();
        if (!"application/pdf".equals(contentType)) throw new IllegalArgumentException("Seuls les fichiers PDF sont acceptés.");
        if (fichier.getSize() > 10_000_000) throw new IllegalArgumentException("Le fichier dépasse la taille maximale de 10 Mo.");
    }
}
