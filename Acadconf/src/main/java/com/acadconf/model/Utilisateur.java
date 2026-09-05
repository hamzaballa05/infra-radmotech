package com.acadconf.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nom;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String prenom;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Size(min = 8)
    @Column(name = "mot_de_passe", nullable = false, length = 255)
    private String motDePasse;

    @Column(length = 150)
    private String affiliation;

    @Column(length = 100)
    private String pays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_compte")
    private StatutCompte statutCompte;

    @OneToMany(mappedBy = "auteurPrincipal")
    private List<Article> articles;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        if (statutCompte == null) statutCompte = StatutCompte.ACTIF;
    }

    public enum Role {
        ADMIN, AUTEUR, PRESIDENT
    }

    public enum StatutCompte {
        ACTIF, INACTIF, BLOQUE, EN_ATTENTE
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getAffiliation() { return affiliation; }
    public void setAffiliation(String affiliation) { this.affiliation = affiliation; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public StatutCompte getStatutCompte() { return statutCompte; }
    public void setStatutCompte(StatutCompte statutCompte) { this.statutCompte = statutCompte; }

    public List<Article> getArticles() { return articles; }
    public void setArticles(List<Article> articles) { this.articles = articles; }

    public String getNomComplet() { return prenom + " " + nom; }
}
