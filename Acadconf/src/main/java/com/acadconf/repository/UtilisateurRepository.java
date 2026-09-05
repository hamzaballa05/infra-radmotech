package com.acadconf.repository;

import com.acadconf.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Utilisateur> findByRole(Utilisateur.Role role);
    List<Utilisateur> findByStatutCompte(Utilisateur.StatutCompte statut);
    List<Utilisateur> findByRoleAndStatutCompte(Utilisateur.Role role, Utilisateur.StatutCompte statut);

    @Query("""
        SELECT u FROM Utilisateur u
        WHERE (:search IS NULL OR :search = '' OR
               LOWER(u.nom) LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(u.prenom) LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(u.affiliation) LIKE LOWER(CONCAT('%',:search,'%')))
          AND (:role IS NULL OR u.role = :role)
          AND (:statut IS NULL OR u.statutCompte = :statut)
        """)
    Page<Utilisateur> findFiltered(
        @Param("search") String search,
        @Param("role") Utilisateur.Role role,
        @Param("statut") Utilisateur.StatutCompte statut,
        Pageable pageable
    );
}
