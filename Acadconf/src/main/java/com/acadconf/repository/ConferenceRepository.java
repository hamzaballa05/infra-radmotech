package com.acadconf.repository;

import com.acadconf.model.Conference;
import com.acadconf.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ConferenceRepository extends JpaRepository<Conference, Long> {
    List<Conference> findByStatut(Conference.Statut statut);
    List<Conference> findByPresident(Utilisateur president);

    @Query("""
        SELECT c FROM Conference c
        WHERE (:search IS NULL OR :search = '' OR
               LOWER(c.titre) LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(c.sigle) LIKE LOWER(CONCAT('%',:search,'%')))
          AND (:statut IS NULL OR c.statut = :statut)
        """)
    Page<Conference> findFiltered(@Param("search") String search,
                                  @Param("statut") Conference.Statut statut,
                                  Pageable pageable);
}
