package com.acadconf.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 250)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String resume;

    @Column(name = "mots_cles", length = 255)
    private String motsCles;

    @Column(name = "fichier_pdf", length = 255)
    private String fichierPdf;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_article")
    private Statut statutArticle;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_conference", nullable = false)
    private Conference conference;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_auteur_principal", nullable = false)
    private Utilisateur auteurPrincipal;

    @PrePersist
    protected void onCreate() {
        dateSoumission = LocalDateTime.now();
        if (statutArticle == null) statutArticle = Statut.SOUMIS;
    }

    public enum Statut {
        SOUMIS, EN_EVALUATION, ACCEPTE, REJETE
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getResume() { return resume; }
    public void setResume(String resume) { this.resume = resume; }

    public String getMotsCles() { return motsCles; }
    public void setMotsCles(String motsCles) { this.motsCles = motsCles; }

    public String getFichierPdf() { return fichierPdf; }
    public void setFichierPdf(String fichierPdf) { this.fichierPdf = fichierPdf; }

    public LocalDateTime getDateSoumission() { return dateSoumission; }
    public void setDateSoumission(LocalDateTime dateSoumission) { this.dateSoumission = dateSoumission; }

    public Statut getStatutArticle() { return statutArticle; }
    public void setStatutArticle(Statut statutArticle) { this.statutArticle = statutArticle; }

    public Conference getConference() { return conference; }
    public void setConference(Conference conference) { this.conference = conference; }

    public Utilisateur getAuteurPrincipal() { return auteurPrincipal; }
    public void setAuteurPrincipal(Utilisateur auteurPrincipal) { this.auteurPrincipal = auteurPrincipal; }

}
