package com.acadconf.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "conference")
public class Conference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String titre;

    @Column(length = 50)
    private String sigle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_debut")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;

    @NotNull
    @Column(name = "date_limite_soumission", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateLimiteSoumission;

    @NotNull
    @Column(name = "date_limite_evaluation", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateLimiteEvaluation;

    @Enumerated(EnumType.STRING)
    private Statut statut;

    @Column(columnDefinition = "TEXT")
    private String themes;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Utilisateur president;

    @OneToMany(mappedBy = "conference", cascade = CascadeType.ALL)
    private List<Article> articles;

    @PrePersist
    protected void onCreate() {
        if (statut == null) statut = Statut.OUVERTE;
    }

    public boolean isSubmissionOpen() {
        return statut == Statut.OUVERTE && LocalDate.now().isBefore(dateLimiteSoumission);
    }

    public boolean isEvaluationOpen() {
        return statut == Statut.EN_EVALUATION && LocalDate.now().isBefore(dateLimiteEvaluation);
    }

    public enum Statut {
        OUVERTE, FERMEE, EN_EVALUATION, TERMINEE
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getSigle() { return sigle; }
    public void setSigle(String sigle) { this.sigle = sigle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public LocalDate getDateLimiteSoumission() { return dateLimiteSoumission; }
    public void setDateLimiteSoumission(LocalDate dateLimiteSoumission) { this.dateLimiteSoumission = dateLimiteSoumission; }

    public LocalDate getDateLimiteEvaluation() { return dateLimiteEvaluation; }
    public void setDateLimiteEvaluation(LocalDate dateLimiteEvaluation) { this.dateLimiteEvaluation = dateLimiteEvaluation; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public String getThemes() { return themes; }
    public void setThemes(String themes) { this.themes = themes; }

    public Utilisateur getPresident() { return president; }
    public void setPresident(Utilisateur president) { this.president = president; }

    public List<Article> getArticles() { return articles; }
    public void setArticles(List<Article> articles) { this.articles = articles; }
}
