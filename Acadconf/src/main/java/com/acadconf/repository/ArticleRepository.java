package com.acadconf.repository;

import com.acadconf.model.Article;
import com.acadconf.model.Conference;
import com.acadconf.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByAuteurPrincipal(Utilisateur auteur);
    Page<Article> findByAuteurPrincipal(Utilisateur auteur, Pageable pageable);
    Page<Article> findAll(Pageable pageable);
    List<Article> findByConference(Conference conference);
    List<Article> findByStatutArticle(Article.Statut statut);
    List<Article> findByConferenceAndStatutArticle(Conference conference, Article.Statut statut);

    @Query("SELECT a FROM Article a WHERE a.conference.id = :conferenceId AND a.statutArticle = :statut")
    List<Article> findByConferenceIdAndStatutArticle(@Param("conferenceId") Long conferenceId,
                                                      @Param("statut") Article.Statut statut);

    @Query("SELECT COUNT(a) FROM Article a WHERE a.conference.id = :conferenceId")
    long countByConferenceId(Long conferenceId);

    @Query("SELECT COUNT(a) FROM Article a WHERE a.conference.id = :conferenceId AND a.statutArticle = :statut")
    long countByConferenceIdAndStatut(Long conferenceId, Article.Statut statut);

    @Query("""
        SELECT a FROM Article a
        WHERE a.auteurPrincipal = :auteur
          AND (:search IS NULL OR :search = '' OR LOWER(a.titre) LIKE LOWER(CONCAT('%',:search,'%')))
          AND (:statut IS NULL OR a.statutArticle = :statut)
          AND (:conferenceId IS NULL OR a.conference.id = :conferenceId)
        """)
    Page<Article> findByAuteurFiltered(@Param("auteur") Utilisateur auteur,
                                       @Param("search") String search,
                                       @Param("statut") Article.Statut statut,
                                       @Param("conferenceId") Long conferenceId,
                                       Pageable pageable);

    @Query("""
        SELECT a FROM Article a
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(a.titre) LIKE LOWER(CONCAT('%',:search,'%'))
               OR LOWER(a.auteurPrincipal.nom) LIKE LOWER(CONCAT('%',:search,'%'))
               OR LOWER(a.auteurPrincipal.prenom) LIKE LOWER(CONCAT('%',:search,'%')))
          AND (:statut IS NULL OR a.statutArticle = :statut)
          AND (:conferenceId IS NULL OR a.conference.id = :conferenceId)
        """)
    Page<Article> findAllFiltered(@Param("search") String search,
                                   @Param("statut") Article.Statut statut,
                                   @Param("conferenceId") Long conferenceId,
                                   Pageable pageable);
}
